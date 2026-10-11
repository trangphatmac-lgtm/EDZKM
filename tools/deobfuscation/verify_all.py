#!/usr/bin/env python3
"""Compile an unmodified Git baseline and run the deobfuscation regression checks.

Requires Python 3 and JDK 21 (entry-point checks use its opt-in SecurityManager).
The application remains Java 8 compatible. No network or test dependency download.
"""
import argparse
import io
import json
from pathlib import Path
import subprocess
import tarfile
import tempfile
import time

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools/deobfuscation"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-ref", default="9931aab23d87a00a738c334adc7156b0c0679aaa", help="Unmodified Git revision")
    args = parser.parse_args()
    records = []
    output = ROOT / "build/deobfuscation"
    output.mkdir(parents=True, exist_ok=True)

    def run(name, command):
        start = time.monotonic()
        result = subprocess.run(command, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        record = {"name": name, "command": list(map(str, command)), "seconds": round(time.monotonic() - start, 3),
                  "returncode": result.returncode, "output": result.stdout}
        records.append(record)
        print(f"{name}: {'PASS' if result.returncode == 0 else 'FAIL'}", flush=True)
        print(result.stdout, end="", flush=True)
        (output / "verification.json").write_text(json.dumps({"baseline": args.baseline_ref, "checks": records}, indent=2))
        if result.returncode:
            raise SystemExit(result.returncode)

    with tempfile.TemporaryDirectory(prefix="zkm-reference-") as temporary:
        work = Path(temporary)
        archive = subprocess.check_output(["git", "archive", args.baseline_ref, "src/main/java"], cwd=ROOT)
        with tarfile.open(fileobj=io.BytesIO(archive)) as source:
            for member in source:
                if member.isfile() and member.name.startswith("src/main/java/") and ".." not in Path(member.name).parts:
                    target = work / member.name
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(source.extractfile(member).read())
        argument_file = work / "sources.txt"
        argument_file.write_text("\n".join('"' + str(path) + '"' for path in sorted((work / "src/main/java").rglob("*.java"))))
        baseline = work / "classes"
        classpath = str(ROOT / "lib/ant-1.10.11.jar") + ":" + str(ROOT / "lib/stubs-wtk.jar")
        run("compile baseline", ["javac", "--release", "8", "-g", "-nowarn", "-encoding", "UTF-8", "-cp", classpath,
                                 "-d", str(baseline), "@" + str(argument_file)])
        run("build current", ["bash", "gradlew", "jar", "--offline"])
        classes = ROOT / "build/classes/java/main"
        test_classes = work / "test-tools"
        tests = ["FlowEquivalenceChecks", "ObfuscatorEquivalence", "ParserEquivalence", "FrontendEquivalence",
                 "EntryPointEquivalenceChecks", "SerializationContracts"]
        run("compile regression tools", ["javac", "-d", str(test_classes), *[str(TOOLS / (name + ".java")) for name in tests]])
        run("isolated algorithm comparisons", ["python3", str(TOOLS / "verify_behavior.py"), "--baseline-ref", args.baseline_ref])
        for name in tests[:-1]:
            command = ["java", "-Xmx512m", "-Djava.awt.headless=true", "-Djava.security.manager=allow", "-cp", str(test_classes),
                       name, str(baseline), str(classes)]
            if name in ("FlowEquivalenceChecks", "EntryPointEquivalenceChecks"):
                command += [str(ROOT / "lib/ant-1.10.11.jar"), str(ROOT / "lib/stubs-wtk.jar")]
            run(name, command)
        manifests = []
        for name, directory in [("baseline", baseline), ("current", classes)]:
            manifest = work / (name + "-serialization.tsv")
            run(name + " serialization contracts", ["java", "-Djava.awt.headless=true", "-cp", str(test_classes),
                                                     "SerializationContracts", str(directory), str(manifest)])
            manifests.append(manifest.read_text())
        if manifests[0] != manifests[1]:
            records.append({"name": "serialization compatibility", "returncode": 1,
                            "output": "Serialization UID or persistent field layout changed"})
            (output / "verification.json").write_text(json.dumps({"baseline": args.baseline_ref, "checks": records}, indent=2))
            raise AssertionError("Serialization UID or persistent field layout changed")
        print("Serialization contracts: identical", flush=True)
        records.append({"name": "serialization compatibility", "returncode": 0, "output": "All UIDs and wire fields identical"})
        (output / "verification.json").write_text(json.dumps({"baseline": args.baseline_ref, "checks": records}, indent=2))


if __name__ == "__main__":
    main()
