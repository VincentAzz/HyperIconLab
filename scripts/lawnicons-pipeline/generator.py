#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""lawnicons 资源处理脚本

从 lawnicons 的 appfilter.xml 生成图标包所需的全部映射产物：
  - icon_mapper/icon_mapper.xml          full 集（App 自身用）
  - icon_mapper/icon_mapper_filtered.xml filtered 集（可选，需提供 app_color_schemes）
  - iconpack_template/appfilter.xml      槽位命名版（APK 产物用，保留 ComponentInfo）
  - iconpack_template/drawable.xml       槽位清单
  - slot_mapping.json                    pkg -> slot 名显式映射
  - version.txt                          版本元信息
  - manifest.json                        分发清单（含 sha256、统计）

稳定槽位原则：跨版本保持 pkg -> slot 映射不变；新增包名追加到末尾；
lawnicons 删除的包名在 history 中保留（便于未来恢复），但不写入新版本 slot_mapping.json。

用法：
  python generator.py \
    --lawnicons-dir /path/to/lawnicons \
    --output-dir /path/to/output \
    [--data-dir /path/to/data] \
    [--history /path/to/slot_mapping_history.json] \
    [--version 20260731] \
    [--lawnicons-commit abc1234] \
    [--pack-zip]

data-dir 应包含（均可选，存在即处理）：
  - icon_mapper_alt.xml      合并进主 mapper（保留 component，覆盖 name/drawable）
  - icon_mapper_preview.xml  原样复制到输出 icon_mapper 目录
  - icon_mapper_test.xml     原样复制到输出 icon_mapper 目录
  - app_color_schemes.xml    用于生成 icon_mapper_filtered.xml
"""

import argparse
import hashlib
import json
import re
import shutil
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Set
from xml.dom import minidom
import xml.etree.ElementTree as ET


# appfilter.xml 中 ComponentInfo 串的正则：ComponentInfo{package/activity}
_COMPONENT_RE = re.compile(r"ComponentInfo\{([^/]+)/([^}]*)\}")


def parse_component(component: str) -> Optional[tuple]:
    """解析 ComponentInfo 串，返回 (package, activity)；失败返回 None"""
    m = _COMPONENT_RE.match(component)
    if m:
        return m.group(1), m.group(2)
    return None


def parse_appfilter(appfilter_path: Path) -> List[dict]:
    """解析 lawnicons appfilter.xml，返回 item 列表

    每个 item: {component, package, activity, name, drawable}
    """
    tree = ET.parse(appfilter_path)
    root = tree.getroot()
    items: List[dict] = []
    for item in root.findall("item"):
        component = item.get("component", "")
        name = item.get("name", "")
        drawable = item.get("drawable", "")
        if not component or not drawable:
            continue
        parsed = parse_component(component)
        if parsed is None:
            continue
        package, activity = parsed
        items.append(
            {
                "component": component,
                "package": package,
                "activity": activity,
                "name": name,
                "drawable": drawable,
            }
        )
    return items


def merge_alt(items: List[dict], alt_path: Optional[Path]) -> List[dict]:
    """合并 icon_mapper_alt.xml 自定义映射

    alt 文件结构：item[name, package, drawable]（无 component）
    合并策略：
      - 原始 items 中已有同 package 条目：保留其 component/activity，
        用 alt 的 name/drawable 覆盖（自定义图标替换默认图标）
      - alt 独有的 package：追加新条目，component 为空
        （此类条目仅进 icon_mapper，不进 iconpack appfilter）
    """
    if alt_path is None or not alt_path.exists():
        return items
    alt_tree = ET.parse(alt_path)
    alt_root = alt_tree.getroot()
    alt_map: Dict[str, dict] = {}
    for item in alt_root.findall("item"):
        package = item.get("package", "")
        if not package:
            continue
        alt_map[package] = {
            "name": item.get("name", ""),
            "drawable": item.get("drawable", ""),
        }
    if not alt_map:
        return items

    merged: List[dict] = []
    for it in items:
        if it["package"] in alt_map:
            alt_entry = alt_map.pop(it["package"])
            # 保留原始 component/activity，覆盖 name/drawable
            merged.append(
                {
                    "component": it["component"],
                    "package": it["package"],
                    "activity": it["activity"],
                    "name": alt_entry["name"],
                    "drawable": alt_entry["drawable"],
                }
            )
        else:
            merged.append(it)

    # alt 中独有的包名追加（component 为空，仅进 icon_mapper）
    for pkg, alt_entry in alt_map.items():
        merged.append(
            {
                "component": "",
                "package": pkg,
                "activity": "",
                "name": alt_entry["name"],
                "drawable": alt_entry["drawable"],
            }
        )
    return merged


def dedupe_and_sort(items: List[dict]) -> List[dict]:
    """按 package 去重（同 package 取最后一条），再按 package 排序"""
    unique: Dict[str, dict] = {}
    for it in items:
        unique[it["package"]] = it
    return [unique[pkg] for pkg in sorted(unique.keys())]


def load_history(history_path: Optional[Path]) -> Dict[str, str]:
    """加载历史槽位映射；文件不存在返回空 dict"""
    if history_path is None or not history_path.exists():
        return {}
    with open(history_path, "r", encoding="utf-8") as f:
        return json.load(f)


def assign_slots(
    sorted_items: List[dict], history: Dict[str, str]
) -> tuple:
    """分配稳定槽位

    返回 (slot_mapping, new_history)
    - slot_mapping: {pkg: slot_name} 仅包含当前存在的包名
    - new_history: {pkg: slot_name} 累加所有历史包名（含已删除）
    """
    # 找出 history 中已用的最大槽位号
    max_slot = 0
    for slot_name in history.values():
        m = re.match(r"slot_(\d+)", slot_name)
        if m:
            max_slot = max(max_slot, int(m.group(1)))

    slot_mapping: Dict[str, str] = {}
    new_history: Dict[str, str] = dict(history)  # 复制，保留所有历史

    next_slot = max_slot + 1
    for it in sorted_items:
        pkg = it["package"]
        if pkg in history:
            slot_mapping[pkg] = history[pkg]
            new_history[pkg] = history[pkg]  # 刷新（值相同）
        else:
            slot_name = f"slot_{next_slot:04d}"
            slot_mapping[pkg] = slot_name
            new_history[pkg] = slot_name
            next_slot += 1

    return slot_mapping, new_history


def _format_xml(root: ET.Element) -> str:
    """格式化 XML：minidom 美化 + 移除空行"""
    rough = ET.tostring(root, encoding="utf-8")
    dom = minidom.parseString(rough)
    pretty = dom.toprettyxml(indent="    ", encoding="utf-8").decode("utf-8")
    lines = [ln for ln in pretty.split("\n") if ln.strip()]
    return "\n".join(lines) + "\n"


def write_icon_mapper(items: List[dict], output_path: Path) -> None:
    """生成 icon_mapper.xml（full 集）：item[name, package, drawable]"""
    root = ET.Element("resources")
    for it in items:
        el = ET.SubElement(root, "item")
        el.set("name", it["name"])
        el.set("package", it["package"])
        el.set("drawable", it["drawable"])
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(_format_xml(root), encoding="utf-8")


def write_icon_mapper_filtered(
    items: List[dict], common_packages: Set[str], output_path: Path
) -> dict:
    """生成 filtered 集：仅保留 common_packages 中的包名

    返回统计 dict
    """
    root = ET.Element("resources")
    filtered_count = 0
    for it in items:
        if it["package"] in common_packages:
            el = ET.SubElement(root, "item")
            el.set("name", it["name"])
            el.set("package", it["package"])
            el.set("drawable", it["drawable"])
            filtered_count += 1
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(_format_xml(root), encoding="utf-8")
    return {
        "total_icons": len(items),
        "common_apps": len(common_packages),
        "filtered_icons": filtered_count,
    }


def write_iconpack_appfilter(
    all_items: List[dict], slot_mapping: Dict[str, str], output_path: Path
) -> int:
    """生成槽位命名版 appfilter.xml（保留所有 ComponentInfo）

    遍历去重前的全部 item，每个 component 映射到其 package 对应的槽位。
    同一 package 的多个 component（多 activity 入口）共用同一槽位，
    保证启动器按 ComponentInfo 精确匹配时所有入口都有图标。
    仅写入有 component 的条目（alt 合并后可能无 component，跳过）。
    返回写入条目数。
    """
    root = ET.Element("resources")
    count = 0
    for it in all_items:
        if not it["component"]:
            continue  # alt 合并的无 ComponentInfo 条目跳过
        slot_name = slot_mapping.get(it["package"])
        if not slot_name:
            continue
        el = ET.SubElement(root, "item")
        el.set("component", it["component"])
        el.set("drawable", slot_name)
        el.set("name", it["name"])
        count += 1
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(_format_xml(root), encoding="utf-8")
    return count


def write_drawable_xml(slot_names: List[str], output_path: Path) -> None:
    """生成 drawable.xml 槽位清单"""
    root = ET.Element("resources")
    for slot_name in sorted(slot_names):
        el = ET.SubElement(root, "item")
        el.set("drawable", slot_name)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(_format_xml(root), encoding="utf-8")


def extract_common_packages(app_color_schemes_path: Path) -> Set[str]:
    """从 app_color_schemes.xml 提取常用应用包名集合"""
    tree = ET.parse(app_color_schemes_path)
    root = tree.getroot()
    packages: Set[str] = set()
    for item in root.findall("item"):
        package = item.get("package")
        if package:
            packages.add(package)
    return packages


def compute_file_sha256(path: Path) -> str:
    """计算文件 sha256"""
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def pack_zip(source_dir: Path, zip_path: Path) -> None:
    """将 source_dir 内容打包为 zip（zip 内不含 source_dir 自身层级）"""
    zip_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for file_path in source_dir.rglob("*"):
            if file_path.is_file():
                arcname = file_path.relative_to(source_dir)
                zf.write(file_path, arcname)


def main():
    parser = argparse.ArgumentParser(
        description="lawnicons 资源处理：生成 icon_mapper 与图标包槽位映射"
    )
    parser.add_argument(
        "--lawnicons-dir",
        required=True,
        help="lawnicons 仓库根目录（含 assets/appfilter.xml 和 svgs/）",
    )
    parser.add_argument(
        "--output-dir", required=True, help="产物输出目录"
    )
    parser.add_argument(
        "--data-dir",
        default=None,
        help="数据目录，含 icon_mapper_alt.xml / icon_mapper_preview.xml / "
        "icon_mapper_test.xml / app_color_schemes.xml。提供后 alt 会合并进主 "
        "mapper，preview/test 原样复制到输出 icon_mapper 目录，"
        "app_color_schemes 用于生成 filtered 集",
    )
    parser.add_argument(
        "--history",
        default=None,
        help="slot_mapping_history.json 路径（稳定槽位历史，可选）",
    )
    parser.add_argument(
        "--version",
        default=None,
        help="版本号（默认取当日 YYYYMMDD）",
    )
    parser.add_argument(
        "--lawnicons-commit",
        default="unknown",
        help="lawnicons git commit hash（CI 传入）",
    )
    parser.add_argument(
        "--pack-zip",
        action="store_true",
        help="打包输出目录为 zip（命名 lawnicons_<version>.zip）",
    )
    args = parser.parse_args()

    lawnicons_dir = Path(args.lawnicons_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # appfilter.xml 路径查找（兼容多种 lawnicons 目录结构）
    # 上游 lawnicons 实际结构：app/assets/appfilter.xml
    appfilter_candidates = [
        lawnicons_dir / "app" / "assets" / "appfilter.xml",
        lawnicons_dir / "assets" / "appfilter.xml",
        lawnicons_dir / "appfilter.xml",
    ]
    appfilter_path = next((p for p in appfilter_candidates if p.exists()), None)
    if appfilter_path is None:
        raise FileNotFoundError(
            f"未找到 appfilter.xml，已尝试: {[str(p) for p in appfilter_candidates]}"
        )

    version = args.version or datetime.now(timezone.utc).strftime("%Y%m%d")

    print("=" * 60)
    print(f"lawnicons 资源处理")
    print(f"  版本: {version}")
    print(f"  lawnicons 目录: {lawnicons_dir}")
    print(f"  appfilter: {appfilter_path}")
    print(f"  输出目录: {output_dir}")
    print("=" * 60)

    # 解析 data-dir 下的各类数据文件路径
    data_dir = Path(args.data_dir) if args.data_dir else None
    alt_path = data_dir / "icon_mapper_alt.xml" if data_dir else None
    preview_path = data_dir / "icon_mapper_preview.xml" if data_dir else None
    test_path = data_dir / "icon_mapper_test.xml" if data_dir else None
    color_schemes_path = data_dir / "app_color_schemes.xml" if data_dir else None

    # 1. 解析 appfilter
    print("\n[1/7] 解析 appfilter.xml")
    items = parse_appfilter(appfilter_path)
    print(f"  解析到 {len(items)} 条 item")

    # 2. 合并 alt（保留原始 component，仅覆盖 name/drawable）
    if alt_path and alt_path.exists():
        print(f"\n[2/7] 合并 icon_mapper_alt: {alt_path}")
        items = merge_alt(items, alt_path)
        print(f"  合并后 {len(items)} 条")
    else:
        print("\n[2/7] 跳过 alt 合并（未提供）")

    # 3. 去重 + 排序
    print("\n[3/7] 去重 + 按包名排序")
    sorted_items = dedupe_and_sort(items)
    print(f"  去重后 {len(sorted_items)} 个包名")

    # 4. 分配稳定槽位
    print("\n[4/7] 分配稳定槽位")
    history_path = Path(args.history) if args.history else None
    history = load_history(history_path)
    print(f"  历史映射 {len(history)} 条")
    slot_mapping, new_history = assign_slots(sorted_items, history)
    new_count = len(slot_mapping) - len(
        {k: v for k, v in slot_mapping.items() if k in history}
    )
    print(f"  当前映射 {len(slot_mapping)} 条，新增 {new_count} 条槽位")

    # 5. 生成 icon_mapper 目录（与 app/src/main/assets/icon_mapper 结构一致）
    mapper_dir = output_dir / "icon_mapper"
    print(f"\n[5/7] 生成 icon_mapper 目录: {mapper_dir}")

    # 5a. 主 mapper（已合并 alt）
    write_icon_mapper(sorted_items, mapper_dir / "icon_mapper.xml")
    print(f"  -> icon_mapper.xml（full 集，{len(sorted_items)} 条）")

    # 5b. filtered 集（基于 app_color_schemes 过滤常用应用）
    if color_schemes_path and color_schemes_path.exists():
        print(f"  生成 filtered 集（基于 {color_schemes_path}）")
        common_packages = extract_common_packages(color_schemes_path)
        stats = write_icon_mapper_filtered(
            sorted_items,
            common_packages,
            mapper_dir / "icon_mapper_filtered.xml",
        )
        print(
            f"  -> icon_mapper_filtered.xml（常用 {stats['common_apps']} 个，"
            f"匹配 {stats['filtered_icons']}/{stats['total_icons']}）"
        )
    else:
        print("  跳过 filtered 集（未提供 app_color_schemes.xml）")

    # 5c. 原样复制 alt / preview / test 到输出 icon_mapper 目录
    # 这些文件是用户维护的子集/覆盖，不参与生成，仅随包分发供 App 端使用
    for name, src in [
        ("icon_mapper_alt.xml", alt_path),
        ("icon_mapper_preview.xml", preview_path),
        ("icon_mapper_test.xml", test_path),
    ]:
        if src and src.exists():
            shutil.copy2(src, mapper_dir / name)
            print(f"  -> {name}（原样复制）")

    # 6. 生成 iconpack_template
    # iconpack appfilter 遍历去重前的全部 item，保留所有 ComponentInfo
    # 同 package 多 component 共用同一槽位，确保启动器所有入口都有图标
    print("\n[6/7] 生成 iconpack_template（槽位命名版 appfilter + drawable.xml）")
    appfilter_count = write_iconpack_appfilter(
        items,
        slot_mapping,
        output_dir / "iconpack_template" / "appfilter.xml",
    )
    print(
        f"  -> appfilter.xml（{appfilter_count} 条 component，"
        f"对应 {len(sorted_items)} 个唯一 package / {len(slot_mapping)} 个槽位）"
    )
    write_drawable_xml(
        list(slot_mapping.values()),
        output_dir / "iconpack_template" / "drawable.xml",
    )
    print(f"  -> drawable.xml（{len(slot_mapping)} 个槽位）")

    # slot_mapping.json
    slot_mapping_path = output_dir / "slot_mapping.json"
    with open(slot_mapping_path, "w", encoding="utf-8") as f:
        json.dump(slot_mapping, f, ensure_ascii=False, indent=2)
    print(f"  -> slot_mapping.json（{len(slot_mapping)} 条映射）")

    # 复制原始 appfilter.xml 与 svgs（供 App 端完整使用）
    orig_appfilter_dst = output_dir / "appfilter.xml"
    shutil.copy2(appfilter_path, orig_appfilter_dst)
    print(f"  -> appfilter.xml（原始 lawnicons 版本，备份）")

    svgs_src = lawnicons_dir / "svgs"
    svgs_dst = output_dir / "svgs"
    if svgs_src.exists():
        if svgs_dst.exists():
            shutil.rmtree(svgs_dst)
        shutil.copytree(svgs_src, svgs_dst)
        svg_count = sum(1 for _ in svgs_dst.rglob("*.svg"))
        print(f"  -> svgs/（{svg_count} 个 svg）")
    else:
        print(f"  [警告] 未找到 svgs 目录: {svgs_src}")

    # 复制 app_color_schemes.xml 到 color_schemes
    # 对齐 app/src/main/assets/color_schemes/
    if color_schemes_path and color_schemes_path.exists():
        color_schemes_dst = output_dir / "color_schemes" / "app_color_schemes.xml"
        color_schemes_dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(color_schemes_path, color_schemes_dst)
        print(f"  -> color_schemes/app_color_schemes.xml")

    # 7. version.txt + manifest.json
    print("\n[7/7] 生成 version.txt 与 manifest.json")
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    version_content = (
        f"version={version}\n"
        f"lawnicons_commit={args.lawnicons_commit}\n"
        f"generated_at={generated_at}\n"
    )
    (output_dir / "version.txt").write_text(version_content, encoding="utf-8")

    manifest = {
        "version": version,
        "lawnicons_commit": args.lawnicons_commit,
        "generated_at": generated_at,
        "stats": {
            "total_icons": len(sorted_items),
            "iconpack_appfilter_items": appfilter_count,
            "filtered": bool(color_schemes_path and color_schemes_path.exists()),
        },
    }
    manifest_path = output_dir / "manifest.json"
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print(f"  -> version.txt")
    print(f"  -> manifest.json")

    # 更新 history
    if history_path:
        history_path.parent.mkdir(parents=True, exist_ok=True)
        with open(history_path, "w", encoding="utf-8") as f:
            json.dump(new_history, f, ensure_ascii=False, indent=2)
        print(f"\n[history] 更新稳定槽位历史: {history_path}（{len(new_history)} 条）")

    # 可选打包 zip
    if args.pack_zip:
        zip_path = output_dir.parent / f"lawnicons_{version}.zip"
        print(f"\n[zip] 打包: {zip_path}")
        pack_zip(output_dir, zip_path)
        zip_sha = compute_file_sha256(zip_path)
        zip_size = zip_path.stat().st_size
        # 补充 manifest 的 package 字段
        manifest["package"] = {
            "filename": zip_path.name,
            "size_bytes": zip_size,
            "sha256": zip_sha,
        }
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump(manifest, f, ensure_ascii=False, indent=2)
        print(f"  -> {zip_path.name}（{zip_size / 1024 / 1024:.2f} MB, sha256={zip_sha[:16]}...）")

    print("\n" + "=" * 60)
    print("完成")
    print(f"  产物目录: {output_dir}")
    print(f"  版本: {version}")
    print(f"  总图标: {len(sorted_items)}")
    print(f"  槽位映射: {len(slot_mapping)} 条")
    print("=" * 60)


if __name__ == "__main__":
    main()
