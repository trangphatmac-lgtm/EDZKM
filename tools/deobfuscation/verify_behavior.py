#!/usr/bin/env python3
"""Compare selected deobfuscated algorithms against a pre-change Git revision.

Usage: python3 tools/deobfuscation/verify_behavior.py --baseline-ref HEAD

Uses only Python's standard library and an installed JDK. All generated Java
sources and class files live in a temporary directory. Small collaborator stubs
make these focused differential checks independent of the application build;
they complement, rather than replace, compilation and integration checks.
"""

import argparse
from pathlib import Path
import re
import shutil
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[2]
FIXTURES = Path(__file__).resolve().parent / "differential"
JAVA_ROOT = "src/main/java/com/zelix/klassmaster/"


def read_source(relative_path, baseline_ref=None):
    path = JAVA_ROOT + relative_path
    if baseline_ref is None:
        return (ROOT / path).read_text()
    return subprocess.check_output(
        ["git", "show", f"{baseline_ref}:{path}"], cwd=ROOT, text=True
    )


def standalone(source):
    source = re.sub(r"^package .*;\n", "", source, flags=re.MULTILINE)
    return re.sub(r"^import com\..*;\n", "", source, flags=re.MULTILINE)


def method_before_next_public(source, signature):
    start = source.index("    public " + signature)
    end = source.index("\n    public ", start + 8)
    return source[start:end]


def prepare_case(workspace, name):
    target = workspace / name
    shutil.copytree(FIXTURES / name, target)
    return target


def check_case(directory):
    subprocess.run(
        ["javac", *map(str, sorted(directory.glob("*.java")))], check=True
    )
    subprocess.run(
        ["java", "-Xmx256m", "-cp", str(directory), "Check"], check=True
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--baseline-ref", default="HEAD", help="Git revision before the refactoring"
    )
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix="zkm-behavior-") as temporary:
        workspace = Path(temporary)
        access = prepare_case(workspace, "access_flags")
        traced = prepare_case(workspace, "traced_type")
        integer = prepare_case(workspace, "int_constant")
        filters = prepare_case(workspace, "rename_filter")
        monitors = prepare_case(workspace, "monitor_matching")
        descriptors = prepare_case(workspace, "descriptors")
        constants = prepare_case(workspace, "constant_usage")
        serial_fields = prepare_case(workspace, "serial_fields")
        attributes = prepare_case(workspace, "attribute_readers")
        for baseline in (True, False):
            ref = args.baseline_ref if baseline else None
            access_name = "BaselineAccessFlags" if baseline else "AccessFlags"
            source = read_source("classfile/AccessFlags.java", ref)
            source = re.sub(r"\bAccessFlags\b", access_name, standalone(source))
            (access / f"{access_name}.java").write_text(source)

            traced_name = "BaselineTracedObjectType" if baseline else "TracedObjectType"
            source = read_source("classfile/insn/TracedObjectType.java", ref)
            source = re.sub(r"\bTracedObjectType\b", traced_name, standalone(source))
            (traced / f"{traced_name}.java").write_text(source)

            integer_name = "BaselineIntBuilder" if baseline else "IntBuilder"
            source = read_source("classfile/insn/Instruction.java", ref)
            method = method_before_next_public(source, "static int appendIntConstant(")
            (integer / f"{integer_name}.java").write_text(
                f"import java.util.*;\nclass {integer_name} extends Instruction {{\n"
                + method
                + "}\n"
            )

            filter_name = "BaselineFilter" if baseline else "CurrentFilter"
            source = read_source("ui/component/RenameFilterModel.java", ref)
            methods = [
                method_before_next_public(source, signature)
                for signature in (
                    "final String toScriptText()",
                    "boolean isMemberPackagePrivate()",
                    "boolean isClassPackagePrivate()",
                )
            ]
            source = re.sub(r"\bRenameFilterModel\b", filter_name, "".join(methods))
            (filters / f"{filter_name}.java").write_text(
                f"import java.util.*;\nclass {filter_name} extends FilterState {{\n"
                + source
                + "}\n"
            )

            frame_name = "BaselineFrame" if baseline else "CurrentFrame"
            source = read_source("classfile/insn/StackFrameState.java", ref)
            method = method_before_next_public(source, "boolean hasMatchingMonitors(")
            method = re.sub(r"\bStackFrameState\b", frame_name, method)
            (monitors / f"{frame_name}.java").write_text(
                f"import java.util.*;\nclass {frame_name} extends FrameState {{\n"
                + method
                + "}\n"
            )

            descriptor_name = "BaselineDescriptors" if baseline else "CurrentDescriptors"
            source = read_source("classfile/constpool/ConstantPoolEntry.java", ref)
            methods = [
                method_before_next_public(source, "static List parseParameterTypes("),
                method_before_next_public(source, "static String descriptorToJavaType(String "),
            ]
            # The one-argument overload occurs earlier in the file.
            descriptor_start = re.search(
                r"    public static String descriptorToJavaType\(String \w+, boolean \w+, boolean ", source
            ).start()
            descriptor_end = source.index("\n    public ", descriptor_start + 8)
            methods[1] = source[descriptor_start:descriptor_end]
            (descriptors / f"{descriptor_name}.java").write_text(
                f"import java.util.*;\nclass {descriptor_name} extends DescriptorState {{\n"
                + "".join(methods)
                + "}\n"
            )

            constants_name = "BaselineConstants" if baseline else "CurrentConstants"
            source = read_source("classfile/insn/MethodBytecode.java", ref)
            method = method_before_next_public(source, "void collectUsedConstants(")
            method = method.replace("com.zelix.klassmaster.classfile.constpool.", "")
            method = method.replace("com.zelix.klassmaster.classfile.insn.", "")
            (constants / f"{constants_name}.java").write_text(
                f"import java.util.*;\nclass {constants_name} extends BytecodeState {{\n"
                + method
                + "}\n"
            )

            serial_name = "BaselineSerialFields" if baseline else "CurrentSerialFields"
            method = method_before_next_public(source, "void collectSerialPersistentFields(")
            (serial_fields / f"{serial_name}.java").write_text(
                f"import java.util.*;\nclass {serial_name} extends BytecodeState {{\n"
                + method
                + "}\n"
            )

            for attribute_name, field, field_type in (
                ("NestMembersAttribute", "memberClasses", "ResolvedClassConstant[]"),
                ("ModulePackagesAttribute", "packages", "ResolvedPackageConstant[]"),
                ("PermittedSubclassesAttribute", "permittedSubclasses", "ResolvedClassConstant[]"),
                ("ParameterAnnotationsAttribute", "parameterAnnotations", "AnnotationEntry[][]"),
            ):
                source = read_source(f"classfile/attribute/{attribute_name}.java", ref)
                start = source.index(f"    public {attribute_name}(")
                end = source.index("\n    @Override", start)
                methods = source[start:end]
                if attribute_name == "ParameterAnnotationsAttribute":
                    start = source.index("    public void collectUsedConstants(")
                    end = source.index("\n    @Override", start)
                    methods += source[start:end]
                name = ("Baseline" if baseline else "Current") + attribute_name
                methods = re.sub(r"\b" + attribute_name + r"\b", name, methods)
                parent = "AnnotationAttributeBase" if attribute_name == "ParameterAnnotationsAttribute" else "ParsedAttributeBase"
                (attributes / f"{name}.java").write_text(
                    f"import java.util.*;\nimport java.io.*;\nclass {name} extends {parent} {{\n"
                    + f"public {field_type} {field};\n" + methods + "}\n"
                )

        for directory in (access, traced, integer, filters, monitors, descriptors, constants, serial_fields, attributes):
            check_case(directory)


if __name__ == "__main__":
    main()
