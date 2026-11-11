#!/usr/bin/env python3
"""Generate Ansible JS/QBC assets with the pinned SDK's native qjsc.

The public API stays TelegramWidget. The native SDK policy carries the same
one-host messenger allowlist as these adapted assets.
"""
import argparse
import pathlib
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--qjsc", required=True, type=pathlib.Path)
    parser.add_argument("--sdk-root", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    args = parser.parse_args()
    source = (args.sdk_root / "packages/desktop/WidgetJS/sandbox-bundle.js").read_bytes()
    source = source.replace(b"\r\n", b"\n")
    hosts = b'["t.me","telegram.me","telegram.dog"]'
    if source.count(hosts) != 1:
        raise RuntimeError("SDK link policy changed; review the Ansible adapter")
    source = source.replace(hosts, b'["asme.su"]')
    # Preserve the space inside the SDK's multiline CSS template without
    # introducing trailing whitespace into the generated source file.
    css_space = b"` { \n"
    if source.count(css_space) != 1:
        raise RuntimeError("SDK CSS template changed; review its source adaptation")
    source = source.replace(css_space, b"` {\\x20\n")

    with tempfile.TemporaryDirectory(prefix="ansible-widget-assets-") as temporary:
        folder = pathlib.Path(temporary)
        script, bytecode = folder / "sandbox-bundle.js", folder / "sandbox-bundle.qbc"
        script.write_bytes(source)
        subprocess.run([str(args.qjsc.resolve()), str(script), str(bytecode),
                        "sandbox-bundle.js"], check=True)
        blob = bytecode.read_bytes()
        digest = 1469598103934665603
        for value in source:
            digest = ((digest ^ value) * 1099511628211) & ((1 << 64) - 1)
        if (blob[:4] != b"TWQB"
                or int.from_bytes(blob[4:8], "little") != 1
                or int.from_bytes(blob[8:16], "little") != len(source)
                or int.from_bytes(blob[16:24], "little") != digest):
            raise RuntimeError("Compiled bytecode does not match the adapted source")
        args.output.mkdir(parents=True, exist_ok=True)
        (args.output / script.name).write_bytes(source)
        (args.output / bytecode.name).write_bytes(blob)


if __name__ == "__main__":
    main()
