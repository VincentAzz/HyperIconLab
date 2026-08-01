#!/usr/bin/env python3
"""检查四种 APK 模板索引并生成独立 Release ZIP。

产物：
    iconpack_templates_<version>.zip
    ├── iconpack-templates-<version>.json
    ├── iconpack-template-full-<version>.apk
    ├── iconpack-template-filtered-<version>.apk
    ├── iconpack-template-preview-<version>.apk
    └── iconpack-template-test-<version>.apk
"""

from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path


EXPECTED_MAPPER_IDS = {"full", "filtered", "preview", "test"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="打包图标包 APK 模板")
    parser.add_argument("--index", required=True, help="模板索引 JSON")
    parser.add_argument("--apk-dir", required=True, help="四个 APK 所在目录")
    parser.add_argument("--output", required=True, help="输出模板 ZIP")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    index_path = Path(args.index).resolve()
    apk_dir = Path(args.apk_dir).resolve()
    output_path = Path(args.output).resolve()
    index = json.loads(index_path.read_text(encoding="utf-8"))
    templates = index.get("templates", {})

    actual_mapper_ids = set(templates)
    if actual_mapper_ids != EXPECTED_MAPPER_IDS:
        missing = sorted(EXPECTED_MAPPER_IDS - actual_mapper_ids)
        extra = sorted(actual_mapper_ids - EXPECTED_MAPPER_IDS)
        raise ValueError(f"模板集合不完整：缺失={missing}，多余={extra}")

    apk_paths: list[Path] = []
    for mapper_id in sorted(EXPECTED_MAPPER_IDS):
        filename = templates[mapper_id].get("filename")
        if not isinstance(filename, str) or Path(filename).name != filename:
            raise ValueError(f"{mapper_id} 模板文件名无效: {filename}")
        apk_path = apk_dir / filename
        if not apk_path.is_file():
            raise FileNotFoundError(f"{mapper_id} 模板不存在: {apk_path}")
        apk_paths.append(apk_path)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output_path, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.write(index_path, index_path.name)
        for apk_path in apk_paths:
            archive.write(apk_path, apk_path.name)

    print(f"模板 ZIP: {output_path}")
    print(f"模板数量: {len(apk_paths)}")


if __name__ == "__main__":
    main()
