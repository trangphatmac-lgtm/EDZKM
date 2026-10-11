# Swing → Web 功能核对表

以 `ZkmMainWindow` 的菜单（`createMenuBar`）、向导和属性面板为基准。下表列出实际 Web 操作入口；Swing 启动按钮仅是备选界面。

| 原入口 / 代码 | Web 入口与行为 | 验证方式 |
| --- | --- | --- |
| File → Open / `OpenClassesDialog` | 项目与 Build Helper 输入：本机浏览、直接路径、归档/目录、嵌套归档、跳过/重新包含条目；类浏览器“打开类” | SnakeGame、输入 JAR 冒烟；原加载器 |
| File → Save all / `SaveClassesTask` | 类浏览器“保存全部”；Build Helper 输出目录、归档压缩、空目录、XML 注释、时间戳 | 最终 JAR 执行冒烟；保存参数语法校验 |
| File → Exit | 右上角退出，停止 worker 和 HTTP 服务 | 启动/退出测试 |
| Tools → Trim / `TrimWizardController` | Build Helper Trim 规则和全部属性选项；类浏览器直接 Trim 当前工作副本 | SnakeGame Trim、原测试报告 |
| `TrimExclusionsDialog` → Test | Trim 页“测试 Trim 报告”，调用 `ClassRepository.reportTrimPreview` | 冒烟测试验证报告 |
| Tools → Obfuscate / `ObfuscateWizardFlow` | Build Helper 混淆保护、命名、元数据、兼容、ChangeLog 分组；类浏览器对当前副本混淆 | CFR/VF 前后结果；全部表单参数通过原解析器 |
| 名称/引用/字符串/数值/控制流/异常/参数排除与包含 | 规则构建器 + Monaco 规则编辑器，采用原 ZKM 规则语法；试运行中查看映射与日志 | 原解析器、真实试运行 |
| Tools → Build Helper / `BuildHelperWizard` | 输入 → Trim → 规则 → 混淆 → 输出 → 脚本预览；跳转任意步骤、运行、试运行 | Playwright + SnakeGame |
| Tools → ZKM Script Helper / `ScriptHelperWizard` | 与 Build Helper 共用配置，“作为脚本编辑”进入多标签编辑器 | 生成脚本语法及交互测试 |
| Tools → Stack Trace translate | 工具页 ChangeLog 列表、混淆字节码路径、字节码分析、粘贴、转换、复制/保存 | 原 `StackTraceTranslator` |
| Tools → ProGuard configuration translate | 工具页打开/粘贴、转换、复制/保存、转入脚本编辑 | 原转换器、冒烟 |
| Options → Classpath | 项目与 Build Helper 依赖列表；依赖变化后 worker 重新加载 | 输入/依赖配置测试 |
| Options → Garbage Collect | 设置中的运行时内存信息与 GC | HTTP 服务测试 |
| Help → Browser help / Overview / About | 设置中的本地帮助、版本与第三方许可 | JAR 资源与本地路由 |
| 包视图 / 继承视图 | 类浏览器可展开包树/继承树；按需加载分支、虚拟滚动、搜索和 `main:` 入口过滤 | 浏览器、树单元测试 |
| `ClassModifyPanel` | 类属性：简单名称、public/package、final、abstract、interface、super | 原重命名/引用更新逻辑 |
| `FieldModifierListModel` | 字段名、访问级别、static/final/volatile/transient | 字段重命名、工作副本恢复、产物执行 |
| `MethodPropertiesPanel` | 方法访问级别、abstract/static/final/synchronized/native；方法名按原编辑器能力保持只读 | 原修饰符约束 |
| `ClassConstantsNode` / `ConstantPoolEntryEditPanel` | 字符串与数值常量；同源字符串副本联动；原常量解析校验 | 修改常量后运行最终 JAR |
| 类声明、成员与字节码文本 | 类详情标签、成员字节码查看；对比页两侧也可切换字节码 | 冒烟 + Playwright |
| Settings 保存 | Web `project.zkm.json`；原 Swing `.ser` 不变 | 修订冲突、重开和工作副本恢复 |

## Web 新增能力

| 功能 | 实现 |
| --- | --- |
| 本机文件操作 | 全部目录/文件浏览使用系统原生选择器；项目页和 Build Helper 输出步骤可打开项目/输出文件夹及导出成品 JAR |
| 双语与主题 | 中英界面、任务/错误摘要；跟随浏览器/系统，可持久指定。脚本关键字、引擎原始日志、原版帮助不翻译 |
| 脚本编辑 | Monaco 高亮/补全、查找替换、撤销、标签页模型保留、保存/另存为、关闭提示、语法标记和定位 |
| 语法校验 | 原预处理器与解析器，不执行 AST |
| 执行隔离 | 独立 JVM、结构化消息、SSE 状态/日志/语句进度、取消进程树、失败恢复 |
| 历史与对比 | 每次运行独立快照、映射和产物；正式/试运行/已有结果导入；手动配对持久化 |
| 离线反编译 | CFR 0.152 / Vineflower 1.12.0；按类启动、依赖上下文、内容缓存、超时和取消 |
| 对比失败恢复 | 两侧独立任务，一侧失败保留另一侧；切换引擎/重试/字节码 |

## 保留的引擎限制

不手动重命名方法；不编辑 multi-release 类及其变体；不手动重命名内部类；接口字段须 public/static/final，final/volatile 不可兼容，abstract 方法遵循原修饰符校验。任意脚本不反向解析为向导表单。预览不改变 `execute` 语义。

自动测试证明其覆盖到的流程，不代表每一种第三方 JAR 和每个规则组合都已经穷举。跨平台 CI 包含 Windows/macOS/Linux × Java 17/21；本机验证记录见提交/交付说明。

## 导航与运行输出

- 类浏览器：默认包树导航（分层展开、键盘操作、保留搜索层级和展开状态），原始 / 混淆后 / 对比三种源码视图，当前配置自动试运行与历史记录选择。
- 页面状态：保留向导步骤、子标签、搜索、滚动位置、工具输入和运行参数。
- 运行输出：默认跟随最新成功构建，离开后返回或停留页面时均自动更新；历史记录/自定义路径可固定，参数和已执行结果保留。原始/混淆后程序各自启动，stdout/stderr 差异、退出码、耗时、主类、参数、标准输入、超时和取消。
