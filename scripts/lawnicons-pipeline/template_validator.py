#!/usr/bin/env python3
"""校验图标包 APK 模板，并生成可供 Release 使用的模板索引。"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import zipfile
from collections import Counter
from pathlib import Path
from typing import Iterable
from xml.etree import ElementTree as ET


MAPPER_FILES = {
    "full": "icon_mapper/icon_mapper.xml",
    "filtered": "icon_mapper/icon_mapper_filtered.xml",
    "preview": "icon_mapper/icon_mapper_preview.xml",
    "test": "icon_mapper/icon_mapper_test.xml",
}
REQUIRED_APK_ENTRIES = {
    "AndroidManifest.xml",
    "resources.arsc",
    "classes.dex",
    "res/xml/appfilter.xml",
    "res/xml/drawable.xml",
    "res/xml/preview_icons.xml",
}
COMPONENT_PATTERN = re.compile(r"ComponentInfo\{([^/}]+)/([^}]+)\}")
PACKAGE_LINE_PATTERN = re.compile(
    r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']*)'"
)
SDK_LINE_PATTERNS = {
    "min_sdk": re.compile(r"minSdkVersion:'([^']+)'"),
    "target_sdk": re.compile(r"targetSdkVersion:'([^']+)'"),
}
RESOURCE_SLOT_PATTERN = re.compile(r"resource 0x[0-9a-fA-F]+ drawable/(slot_\d+)")
XML_ATTRIBUTE_PATTERN = re.compile(r'^\s*A:\s+([^=]+)="(.*?)"\s+\(Raw:')
TYPED_XML_ATTRIBUTE_PATTERN = re.compile(r"^\s*A:\s+([^=]+)=(.*?)\s*$")
SIGNATURE_SUFFIXES = (".RSA", ".DSA", ".EC", ".SF")


class ValidationError(RuntimeError):
    """模板校验失败。"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="校验图标包 APK 模板")
    parser.add_argument("--bundle", required=True, help="lawnicons_<version>.zip 路径")
    parser.add_argument("--apk", required=True, help="待校验的未签名模板 APK")
    parser.add_argument("--mapper-id", choices=sorted(MAPPER_FILES), required=True)
    parser.add_argument("--expected-application-id", required=True)
    parser.add_argument("--aapt2", help="aapt2 可执行文件；省略时从 ANDROID_HOME 查找")
    parser.add_argument("--apksigner", help="apksigner 可执行文件；省略时从 ANDROID_HOME 查找")
    parser.add_argument("--output-index", required=True, help="输出模板索引 JSON")
    return parser.parse_args()


def find_build_tool(explicit: str | None, tool_names: Iterable[str]) -> Path:
    if explicit:
        path = Path(explicit).resolve()
        if path.is_file():
            return path
        raise ValidationError(f"构建工具不存在: {path}")

    sdk_root = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk_root:
        raise ValidationError("未设置 ANDROID_HOME/ANDROID_SDK_ROOT，且未显式指定构建工具")

    build_tools_dir = Path(sdk_root) / "build-tools"
    if not build_tools_dir.is_dir():
        raise ValidationError(f"Android build-tools 目录不存在: {build_tools_dir}")

    def version_key(path: Path) -> tuple[int, ...]:
        numbers = re.findall(r"\d+", path.name)
        return tuple(int(number) for number in numbers)

    for version_dir in sorted(build_tools_dir.iterdir(), key=version_key, reverse=True):
        for tool_name in tool_names:
            candidate = version_dir / tool_name
            if candidate.is_file():
                return candidate
    raise ValidationError(f"未在 {build_tools_dir} 找到构建工具: {', '.join(tool_names)}")


def run_tool(command: list[str], allow_failure: bool = False) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if result.returncode != 0 and not allow_failure:
        details = (result.stderr or result.stdout).strip()
        raise ValidationError(f"工具执行失败（{result.returncode}）: {' '.join(command)}\n{details}")
    return result


def parse_binary_xml(aapt2: Path, apk: Path, xml_path: str) -> list[dict[str, str]]:
    output = run_tool([str(aapt2), "dump", "xmltree", str(apk), "--file", xml_path]).stdout
    items: list[dict[str, str]] = []
    current: dict[str, str] | None = None
    for line in output.splitlines():
        if re.match(r"^\s*E: item\b", line):
            if current is not None:
                items.append(current)
            current = {}
            continue
        if current is None:
            continue
        match = XML_ATTRIBUTE_PATTERN.match(line)
        if match:
            current[match.group(1).strip()] = match.group(2)
            continue
        # 纯数字名称会被 AAPT2 编译为整数，不再包含 Raw 字符串。
        typed_match = TYPED_XML_ATTRIBUTE_PATTERN.match(line)
        if typed_match:
            current[typed_match.group(1).strip()] = typed_match.group(2).strip('"')
    if current is not None:
        items.append(current)
    return items


def load_bundle_inputs(bundle: Path, mapper_id: str) -> dict[str, object]:
    with zipfile.ZipFile(bundle) as archive:
        mapper_root = ET.fromstring(archive.read(MAPPER_FILES[mapper_id]))
        slot_mapping = json.loads(archive.read("slot_mapping.json"))
        appfilter_root = ET.fromstring(archive.read("appfilter.xml"))
        manifest = json.loads(archive.read("manifest.json"))

    mapper_items: dict[str, dict[str, str]] = {}
    for item in mapper_root.findall("item"):
        package_name = item.get("package")
        if not package_name:
            continue
        mapper_items.setdefault(
            package_name,
            {
                "name": item.get("name", package_name),
                "drawable": item.get("drawable", ""),
            },
        )

    missing_slots = sorted(set(mapper_items) - set(slot_mapping))
    if missing_slots:
        raise ValidationError(f"Bundle mapper 存在未分配槽位的包名: {missing_slots}")

    selected_packages = set(mapper_items)
    expected_appfilter: list[dict[str, str]] = []
    for item in appfilter_root.findall("item"):
        component = item.get("component", "")
        match = COMPONENT_PATTERN.fullmatch(component)
        if not match or match.group(1) not in selected_packages:
            continue
        package_name = match.group(1)
        expected_appfilter.append(
            {
                "component": component,
                "drawable": slot_mapping[package_name],
                "name": item.get("name", mapper_items[package_name]["name"]),
            }
        )

    expected_preview = [
        {
            "name": item["name"],
            "package": package_name,
            "drawable": slot_mapping[package_name],
        }
        for package_name, item in mapper_items.items()
    ]
    expected_slots = {slot_mapping[package_name] for package_name in selected_packages}
    history_max_slot = max(int(slot.rsplit("_", 1)[1]) for slot in slot_mapping.values())

    return {
        "manifest": manifest,
        "expected_slots": expected_slots,
        "expected_appfilter": expected_appfilter,
        "expected_preview": expected_preview,
        "history_max_slot": history_max_slot,
    }


def assert_equal(label: str, actual: object, expected: object) -> None:
    if actual != expected:
        raise ValidationError(f"{label} 不一致：实际={actual!r}，期望={expected!r}")


def assert_item_multiset(
    label: str,
    actual: list[dict[str, str]],
    expected: list[dict[str, str]],
    fields: tuple[str, ...],
) -> None:
    actual_counter = Counter(tuple(item.get(field, "") for field in fields) for item in actual)
    expected_counter = Counter(tuple(item.get(field, "") for field in fields) for item in expected)
    if actual_counter == expected_counter:
        return
    missing = list((expected_counter - actual_counter).elements())[:5]
    extra = list((actual_counter - expected_counter).elements())[:5]
    raise ValidationError(f"{label} 不一致：缺失示例={missing}，多余示例={extra}")


def compute_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate() -> tuple[dict[str, object], str]:
    args = parse_args()
    bundle = Path(args.bundle).resolve()
    apk = Path(args.apk).resolve()
    output_index = Path(args.output_index).resolve()
    if not bundle.is_file():
        raise ValidationError(f"Bundle 不存在: {bundle}")
    if not apk.is_file():
        raise ValidationError(f"模板 APK 不存在: {apk}")

    aapt2 = find_build_tool(args.aapt2, ("aapt2.exe", "aapt2"))
    apksigner = find_build_tool(args.apksigner, ("apksigner.bat", "apksigner"))
    bundle_inputs = load_bundle_inputs(bundle, args.mapper_id)
    expected_slots = bundle_inputs["expected_slots"]

    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        duplicates = sorted(name for name, count in Counter(names).items() if count > 1)
        if duplicates:
            raise ValidationError(f"APK 包含重复 ZIP 条目: {duplicates[:5]}")

        missing_entries = sorted(REQUIRED_APK_ENTRIES - set(names))
        if missing_entries:
            raise ValidationError(
                "APK 缺少固定条目，资源路径可能被优化或模板不完整: "
                f"{missing_entries}"
            )

        signature_entries = [
            name
            for name in names
            if name.startswith("META-INF/") and name.upper().endswith(SIGNATURE_SUFFIXES)
        ]
        if signature_entries:
            raise ValidationError(f"模板包含旧 v1 签名条目: {signature_entries}")

        actual_slot_paths = {
            name.removeprefix("res/drawable/").removesuffix(".png")
            for name in names
            if name.startswith("res/drawable/slot_") and name.endswith(".png")
        }
    assert_equal("APK ZIP 槽位路径", actual_slot_paths, expected_slots)

    verify_result = run_tool(
        [str(apksigner), "verify", "--verbose", str(apk)],
        allow_failure=True,
    )
    if verify_result.returncode == 0:
        raise ValidationError("模板 APK 已存在有效签名，必须使用未签名模板")

    badging = run_tool([str(aapt2), "dump", "badging", str(apk)]).stdout
    package_match = PACKAGE_LINE_PATTERN.search(badging)
    if not package_match:
        raise ValidationError("无法从 APK 读取 applicationId/version")
    application_id, version_code, version_name = package_match.groups()
    assert_equal("applicationId", application_id, args.expected_application_id)

    sdk_values: dict[str, str] = {}
    for key, pattern in SDK_LINE_PATTERNS.items():
        match = pattern.search(badging)
        if not match:
            raise ValidationError(f"无法从 APK 读取 {key}")
        sdk_values[key] = match.group(1)

    resources_dump = run_tool([str(aapt2), "dump", "resources", str(apk)]).stdout
    resource_slots = set(RESOURCE_SLOT_PATTERN.findall(resources_dump))
    assert_equal("resources.arsc 槽位", resource_slots, expected_slots)

    actual_appfilter = parse_binary_xml(aapt2, apk, "res/xml/appfilter.xml")
    actual_drawable = parse_binary_xml(aapt2, apk, "res/xml/drawable.xml")
    actual_preview = parse_binary_xml(aapt2, apk, "res/xml/preview_icons.xml")
    assert_item_multiset(
        "二进制 appfilter",
        actual_appfilter,
        bundle_inputs["expected_appfilter"],
        ("component", "drawable", "name"),
    )
    assert_item_multiset(
        "二进制 drawable",
        actual_drawable,
        [{"drawable": slot} for slot in expected_slots],
        ("drawable",),
    )
    assert_item_multiset(
        "二进制 preview_icons",
        actual_preview,
        bundle_inputs["expected_preview"],
        ("name", "package", "drawable"),
    )

    bundle_manifest = bundle_inputs["manifest"]
    template_info = {
        "filename": apk.name,
        "mapper_file": MAPPER_FILES[args.mapper_id].rsplit("/", 1)[-1],
        "application_id": application_id,
        "version_code": int(version_code),
        "version_name": version_name,
        "min_sdk": int(sdk_values["min_sdk"]),
        "target_sdk": int(sdk_values["target_sdk"]),
        "slot_count": len(expected_slots),
        "appfilter_item_count": len(actual_appfilter),
        "preview_item_count": len(actual_preview),
        "size_bytes": apk.stat().st_size,
        "sha256": compute_sha256(apk),
    }
    index = {
        "schema_version": 1,
        "resource_version": bundle_manifest["version"],
        "lawnicons_commit": bundle_manifest["lawnicons_commit"],
        "generated_at": bundle_manifest["generated_at"],
        "history_max_slot": bundle_inputs["history_max_slot"],
        "templates": {args.mapper_id: template_info},
    }
    if output_index.is_file():
        existing_index = json.loads(output_index.read_text(encoding="utf-8"))
        metadata_keys = (
            "schema_version",
            "resource_version",
            "lawnicons_commit",
            "generated_at",
            "history_max_slot",
        )
        for key in metadata_keys:
            assert_equal(f"已有索引 {key}", existing_index.get(key), index[key])
        existing_templates = existing_index.get("templates")
        if not isinstance(existing_templates, dict):
            raise ValidationError("已有模板索引的 templates 字段格式无效")
        index["templates"] = {**existing_templates, args.mapper_id: template_info}
    output_index.parent.mkdir(parents=True, exist_ok=True)
    output_index.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return index, args.mapper_id


def main() -> int:
    try:
        index, mapper_id = validate()
    except (ValidationError, KeyError, ValueError, zipfile.BadZipFile, ET.ParseError) as error:
        print(f"[模板校验失败] {error}", file=sys.stderr)
        return 1

    template_info = index["templates"][mapper_id]
    print("[模板校验通过]")
    print(f"  资源版本: {index['resource_version']}")
    print(f"  模板集合: {mapper_id}")
    print(f"  槽位数量: {template_info['slot_count']}")
    print(f"  appfilter: {template_info['appfilter_item_count']}")
    print(f"  APK 大小: {template_info['size_bytes']} bytes")
    print(f"  SHA-256: {template_info['sha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
