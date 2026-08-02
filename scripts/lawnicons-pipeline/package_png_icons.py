import argparse
import zipfile
import xml.etree.ElementTree as ElementTree
from pathlib import Path


PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=Path, required=True, help="PNG 源目录")
    parser.add_argument("--bundle", type=Path, required=True, help="Lawnicons bundle ZIP")
    parser.add_argument("--mapper-id", required=True, help="icon_mapper 集合标识")
    parser.add_argument("--output", type=Path, required=True, help="输出 icons ZIP")
    return parser.parse_args()


def load_mapper_packages(bundle_path: Path, mapper_id: str) -> list[str]:
    mapper_path = f"icon_mapper/icon_mapper_{mapper_id}.xml"
    with zipfile.ZipFile(bundle_path) as bundle:
        root = ElementTree.fromstring(bundle.read(mapper_path))

    # 保持 mapper 顺序，同时去除同一包名的重复 activity 映射。
    packages: list[str] = []
    seen: set[str] = set()
    for item in root.findall("item"):
        package_name = item.get("package")
        if package_name and package_name not in seen:
            packages.append(package_name)
            seen.add(package_name)
    return packages


def package_pngs(source_dir: Path, packages: list[str]) -> dict[str, Path]:
    source_files = {path.stem: path for path in source_dir.glob("*.png")}
    unexpected = sorted(set(source_files) - set(packages))
    if unexpected:
        raise ValueError(f"PNG 目录包含当前 mapper 未使用的包名: {unexpected}")

    selected = {package: source_files[package] for package in packages if package in source_files}
    missing = [package for package in packages if package not in source_files]
    if missing:
        print(f"警告：{len(missing)} 个 mapper 包名缺少 PNG，将保留模板占位槽位: {missing}")
    if not selected:
        raise ValueError("PNG 目录没有可匹配当前 mapper 的文件")
    return selected


def write_icons_zip(selected: dict[str, Path], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output_path, "w", zipfile.ZIP_DEFLATED) as output:
        for package_name, path in selected.items():
            content = path.read_bytes()
            if not content.startswith(PNG_SIGNATURE):
                raise ValueError(f"文件不是有效 PNG: {path}")
            output.writestr(f"icons/{package_name}.png", content)


def main() -> None:
    args = parse_args()
    if not args.source_dir.is_dir():
        raise FileNotFoundError(f"PNG 源目录不存在: {args.source_dir}")
    if not args.bundle.is_file():
        raise FileNotFoundError(f"bundle 不存在: {args.bundle}")

    packages = load_mapper_packages(args.bundle, args.mapper_id)
    selected = package_pngs(args.source_dir, packages)
    write_icons_zip(selected, args.output)
    print(
        f"已打包 {len(selected)} 个 PNG（mapper 唯一包名 {len(packages)} 个）: "
        f"{args.output.resolve()}"
    )


if __name__ == "__main__":
    main()
