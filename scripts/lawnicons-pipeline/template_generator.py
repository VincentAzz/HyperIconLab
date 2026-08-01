#!/usr/bin/env python3
"""根据 Lawnicons Bundle 为图标包模板生成 Android 资源。"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import struct
import zipfile
import zlib
from pathlib import Path
from xml.etree import ElementTree as ET
from xml.dom import minidom


MAPPER_FILES = {
    "full": "icon_mapper/icon_mapper.xml",
    "filtered": "icon_mapper/icon_mapper_filtered.xml",
    "preview": "icon_mapper/icon_mapper_preview.xml",
    "test": "icon_mapper/icon_mapper_test.xml",
}
COMPONENT_PATTERN = re.compile(r"ComponentInfo\{([^/}]+)/([^}]+)\}")
AAPT_TYPED_NUMBER_PATTERN = re.compile(
    r"^[+-]?(?:0x[0-9a-fA-F]+|(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?)"
    r"(?:dp|dip|sp|px|pt|in|mm|%)?$"
)


def format_xml(root: ET.Element) -> bytes:
    rough = ET.tostring(root, encoding="utf-8")
    dom = minidom.parseString(rough)
    pretty = dom.toprettyxml(indent="    ", encoding="utf-8").decode("utf-8")
    lines = [line for line in pretty.splitlines() if line.strip()]
    return ("\n".join(lines) + "\n").encode("utf-8")


def png_chunk(kind: bytes, data: bytes) -> bytes:
    payload = kind + data
    return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload))


def create_placeholder_png(slot_number: int) -> bytes:
    """生成内容可区分的 2×2 近透明 PNG，降低 AAPT2 资源去重风险。"""
    red = slot_number & 0xFF
    green = (slot_number >> 8) & 0xFF
    blue = (slot_number >> 16) & 0xFF
    pixel = bytes((red, green, blue, 1))
    raw = b"".join(b"\x00" + pixel * 2 for _ in range(2))
    return b"".join(
        (
            b"\x89PNG\r\n\x1a\n",
            png_chunk(b"IHDR", struct.pack(">IIBBBBB", 2, 2, 8, 6, 0, 0, 0)),
            png_chunk(b"IDAT", zlib.compress(raw, level=9)),
            png_chunk(b"IEND", b""),
        )
    )


def escape_android_string(value: str) -> str:
    """阻止 AAPT2 将纯数字、尺寸等名称推断为非字符串类型。"""
    if AAPT_TYPED_NUMBER_PATTERN.fullmatch(value):
        return f"\\{value}"
    return value


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="生成图标包 APK 模板资源")
    parser.add_argument("--bundle", required=True, help="lawnicons_<version>.zip 路径")
    parser.add_argument("--mapper-id", choices=sorted(MAPPER_FILES), default="test")
    parser.add_argument("--project-dir", required=True, help="template/iconpack-template 路径")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    bundle = Path(args.bundle).resolve()
    project_dir = Path(args.project_dir).resolve()
    output_res = project_dir / "app" / "build" / "generated" / "iconpack" / args.mapper_id / "res"

    if not bundle.is_file():
        raise FileNotFoundError(f"Bundle 不存在: {bundle}")
    if not (project_dir / "app" / "build.gradle.kts").is_file():
        raise FileNotFoundError(f"模板工程不存在: {project_dir}")

    if output_res.exists():
        shutil.rmtree(output_res)
    (output_res / "xml").mkdir(parents=True)
    (output_res / "drawable").mkdir(parents=True)

    with zipfile.ZipFile(bundle) as archive:
        mapper_root = ET.fromstring(archive.read(MAPPER_FILES[args.mapper_id]))
        slot_mapping = json.loads(archive.read("slot_mapping.json"))
        appfilter_root = ET.fromstring(archive.read("appfilter.xml"))

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
        raise ValueError(f"mapper 中存在未分配槽位的包名: {missing_slots}")

    selected_packages = set(mapper_items)
    generated_appfilter = ET.Element("resources")
    component_count = 0
    for item in appfilter_root.findall("item"):
        component = item.get("component", "")
        match = COMPONENT_PATTERN.fullmatch(component)
        if not match or match.group(1) not in selected_packages:
            continue
        package_name = match.group(1)
        output_item = ET.SubElement(generated_appfilter, "item")
        output_item.set("component", component)
        output_item.set("drawable", slot_mapping[package_name])
        output_item.set(
            "name",
            escape_android_string(item.get("name", mapper_items[package_name]["name"])),
        )
        component_count += 1

    selected_slots = sorted(
        {slot_mapping[package_name] for package_name in selected_packages},
        key=lambda value: int(value.rsplit("_", 1)[1]),
    )
    generated_drawable = ET.Element("resources")
    generated_preview = ET.Element("resources")
    for slot_name in selected_slots:
        item = ET.SubElement(generated_drawable, "item")
        item.set("drawable", slot_name)

        slot_number = int(slot_name.rsplit("_", 1)[1])
        (output_res / "drawable" / f"{slot_name}.png").write_bytes(
            create_placeholder_png(slot_number)
        )

    for package_name, mapper_item in mapper_items.items():
        item = ET.SubElement(generated_preview, "item")
        item.set("name", escape_android_string(mapper_item["name"]))
        item.set("package", package_name)
        item.set("drawable", slot_mapping[package_name])

    (output_res / "xml" / "appfilter.xml").write_bytes(format_xml(generated_appfilter))
    (output_res / "xml" / "drawable.xml").write_bytes(format_xml(generated_drawable))
    (output_res / "xml" / "preview_icons.xml").write_bytes(format_xml(generated_preview))

    print(f"模板集合: {args.mapper_id}")
    print(f"唯一包名: {len(selected_packages)}")
    print(f"槽位资源: {len(selected_slots)}")
    print(f"appfilter component: {component_count}")
    print(f"生成目录: {output_res}")


if __name__ == "__main__":
    main()
