import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import gen
import mcformat


def main():
    profile = mcformat.init()
    out = mcformat.out_root
    if out.exists():
        if mcformat.out_overridden and out == gen.common.ROOT:
            raise SystemExit("refusing to clear the repository root")
        shutil.rmtree(out)
    gen.build(profile, out)
    count = sum(1 for _ in out.rglob("*.json"))
    print(f"generated {count} files")


if __name__ == "__main__":
    main()
