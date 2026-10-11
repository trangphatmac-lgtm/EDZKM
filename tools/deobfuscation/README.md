# 反混淆修改与验证记录

本轮基于提交 `9931aab23d87a00a738c334adc7156b0c0679aaa`，于 2026-10-08 完成全仓库 Java 语法树与符号分析，并对存在残留跳转结构的实现逐项整理。源代码已直接修改；此目录保存维护工具、差分验证和审计记录。仓库原有 README 修改未覆盖。

**这是已经验证的一轮大规模清理，尚不能称为全部命名和全部控制流反混淆完成。** 原始源码名称无法由编译产物唯一恢复；这里使用有证据支持的可读名称。审计仍保留 11,350 个短名/通用名候选，其中也有合法的循环变量、`id` 和词法常量缩写。不能把候选数当作剩余混淆点的精确数量，自动推断没有新建议也不代表代码已经完全可读。

## 修改范围

- 分析 1,551 个 Java 源文件，最终审计覆盖 57,566 个变量声明。
- 工具记录 8,431 项符号重命名：8,422 个字段、参数或局部变量，加 9 个方法。包含 28 个实际重命名字段；另有人工结构化改写时完成的命名，不计入该数字。
- 392 处反编译标签中，388 处通过条件分支、循环、提前返回或语法解析辅助方法消除。其余四处属于词法分析器的外层扫描循环，改为有意义的 `tokenScan`。这项计数只针对标签，不衡量所有可能的控制流混淆。
- 简化 167 个经类型归属检查的布尔三元表达式，保留操作数求值次数、先后顺序、短路和拆箱行为。
- 涉及 classfile/字节码分析、obfuscator、engine、archive、XML、脚本 AST/解析器、changelog、ProGuard 解析、UI 和启动入口。

`RecoverNames.java` 使用 javac 的符号绑定更新声明和引用，不对文本做全局替换，不改字符串或注释。推断依据包括赋值、访问器、方法实参角色、数组下标和声明类型。冲突通过作用域内保留名字解决；报告记录每一项推断依据。类型或参数角色只能提供有限语义，诸如带 `Argument`、`Value` 后缀的名称仍可继续人工改善。

动态控制标记仍可改变分支，不能假设它们恒定后删除路径。原代码的异常吞掉行为、重复关闭、关闭失败、特殊输入下的异常乃至不终止路径均按原实现保留；没有借本次整理顺便修正业务行为。

## 序列化与兼容性

Java 默认序列化把实例字段名写入存储格式，所以 131 个符合短名规则的持久化字段保留实际名称。已为其中有可靠含义的字段补充角色注释；完整角色映射有 82 项。要让这些字段的 Java 名称也全部可读，需要单独设计、验证旧存储格式适配，不能直接批量改名。

公开静态或 transient 字段改名也会影响默认 `serialVersionUID`。以下六个类固定为原版 ID：`ObfuscateOptions`、`MixedCaseNamesMode`、`ChangeLogInputFile`、`PersistedStringList`、`WizardAppSettings`、`SerialVersionUidCalculator`。最终 196 个可序列化类的 ID 和持久化字段名称、类型与基线逐项一致。

仓库内引用已经同步更新。对仓库外按旧名称调用的二进制代码、按字符串反射访问成员的程序，成员改名不承诺兼容；调试信息和堆栈中的方法名称也会随命名变化。序列化布局检查不等于所有历史存档的完整应用级往返测试。

## 可复跑的验证

在仓库根目录运行，使用 Python 3 与 JDK 21；应用本身仍编译为 Java 8：

```sh
python3 tools/deobfuscation/verify_all.py --baseline-ref 9931aab23d87a00a738c334adc7156b0c0679aaa
```

脚本从 Git 导出并编译原版、离线构建当前 JAR、运行差分验证并比较序列化契约。依赖仓库 `lib/` 和已缓存的 Gradle 8.14.5，不下载测试框架。入口测试在子 JVM 中使用 `-Djava.security.manager=allow` 拦截退出；因此应使用 JDK 21，不适用于已移除 SecurityManager 的新 JDK。最新运行日志写入 `build/deobfuscation/verification.json`。

本轮全部通过，共 **14,150,788** 项差分比较：

| 验证 | 比较数 | 实际范围 |
| --- | ---: | --- |
| `verify_behavior.py` | 14,096,612 | 访问标记、追踪类型、整数常量指令、重命名筛选、监视器匹配、描述符、常量引用、持久化字段扫描、属性读取。使用抽取方法或完整类及轻量协作者桩，是隔离算法测试。 |
| `FlowEquivalenceChecks` | 13,946 | XML、资源/文件、归档关闭与删除、属性、AST 格式和动态控制标记；原版与新版使用独立类加载器。 |
| `ObfuscatorEquivalence` | 34,868 | 位排列、参数布局、反射值、辅助类分配，包含原版异常路径。 |
| `ParserEquivalence` | 1,885 | changelog/ProGuard AST、解析错误、前瞻、字符流、筛选和列表。 |
| `FrontendEquivalence` | 3,458 | 脚本解析及截断输入、错误状态、文本折行、诊断和类型比较。 |
| `EntryPointEquivalenceChecks` | 19 | 启动输出、退出、重定向失败；用替身入口避免启动真实界面。 |

这些检查比较返回值、异常以及所覆盖的调用顺序/状态。它们不替代完整 GUI 操作、真实项目混淆和全部环境组合的端到端测试，也不能证明整个应用所有输入行为等价。

另外，本次会话在控制流改写结束、名称修改开始前保存了编译快照，使用 `VerifyRenaming.java` 对比最终 1,552 个编译类。逆向还原 37 项成员命名，剔除源码/调试元数据和独立核对的 UID 常量后，可执行内容全部一致。此结论只覆盖**命名阶段**，不声称控制流重写前后字节码相同。快照位于忽略目录 `build/deobfuscation/before-names-classes`，并非可由原始 Git 基线直接重建的独立提交；完整差分验证脚本不依赖此快照。

命名验证工具需要 ASM（本次使用本地 Gradle 自带的 ASM 9.8）。保留会话快照时可运行：

```sh
# 将 <gradle-lib> 替换为本机 Gradle 8.14.5 的 lib 目录。
javac -proc:none -cp '<gradle-lib>/*' -d build/deobfuscation/tools tools/deobfuscation/VerifyRenaming.java
java -cp 'build/deobfuscation/tools:<gradle-lib>/*' VerifyRenaming build/deobfuscation/before-names-classes build/classes/java/main tools/deobfuscation/reports/renamed-members.tsv
```

## 审计文件与后续工作

| 文件 | 内容 |
| --- | --- |
| [names-pass-1.tsv](reports/names-pass-1.tsv)、[names-pass-2.tsv](reports/names-pass-2.tsv) | 两轮实际应用的变量/字段映射和依据；行号是各轮修改前的位置。 |
| [renamed-members.tsv](reports/renamed-members.tsv) | 实际应用的 28 个字段、9 个方法映射。 |
| [serialized-field-roles.tsv](reports/serialized-field-roles.tsv) | 保留旧存储名的字段所对应的可读角色。 |
| [remaining-name-candidates.tsv](reports/remaining-name-candidates.tsv) | 最终候选清单：6,023 个参数、5,175 个局部变量、152 个字段，其中 131 个字段因序列化保留名称。源码路径相对 `src/main/java/`。 |
| [boolean-simplifications.tsv](reports/boolean-simplifications.tsv) | 167 项布尔表达式改写，包含前后文本。 |
| [serialization-contracts.tsv](reports/serialization-contracts.tsv) | 与原版一致的 196 项序列化契约。 |
| [verification.json](reports/verification.json) | 本轮通过的运行结果与统计；本机仓库路径、临时目录路径已替换为占位符。 |

重新运行只读命名审计：

```sh
javac -d build/deobfuscation/tools tools/deobfuscation/RecoverNames.java
java -Xmx2g -cp build/deobfuscation/tools RecoverNames src/main/java build/deobfuscation/name-audit tools/deobfuscation/member-names.tsv
```

不带 `--apply` 不写源码。`member-names.tsv` 是经过查看代码后添加的角色映射；持久化字段映射只用于注释和数据流推断。若继续修改，应从候选清单逐个方法确认语义，补充明确映射，保留修改前编译快照并复跑相关验证。不要通过把所有短名机械替换为 `value1` 或强制消除候选计数来宣称完成。

`SimplifyBooleanFlow.java` **会直接修改**传入源码目录；第二个参数为输出 TSV 路径。它是本轮应用过的变换工具，不是只读审计入口。使用前应保存基线。
