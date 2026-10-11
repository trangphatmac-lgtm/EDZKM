# 本次验证记录

验证日期：2026-10-11；本机 macOS，Java 21.0.5 / Zulu 17.0.17。

- `sh gradlew --offline test jar`：通过；17 项 JUnit 测试，包含原生对话框 API 的令牌/同源检查、取消、Unicode 路径、成功产物筛选、JAR 字节保真复制、目录打包的 Manifest/资源与避免递归打包自身，以及 worker 强制退出、程序 stdout/stderr、中文标准输入、参数、执行目录、非零退出码、超时和输出截断。
- `npm --prefix web test`：通过；11 项 Vitest 测试，包含配置变更、原始视图缓存、任务复用、嵌套包树、多来源/MR 类身份、搜索祖先路径和混淆映射。
- `npm --prefix web run test:e2e`：通过；真实打包 JAR + Chromium，系统选择器 API 调用/取消（原生 UI 使用替身）、打开项目/输出文件夹及导出按钮、最新成功构建自动选择（首次无记录、离开页面期间、当前页面、手动固定历史再恢复自动、参数/标准输入保留）、项目、脚本、类浏览器包树（多层包、键盘展开、折叠状态保留、搜索、点击类反编译）、三种源码视图、自动试运行、同配置复用、失败后手动重试、步骤/筛选/编辑标签保留、两种反编译器、真实程序输出对比（参数、中文输入、资源、一侧失败）、中英、深浅主题和刷新。测试主动阻断非本机请求，未发现外部资源请求。
- Java 17 / Java 21 分别运行 `tests/run_smoke.py`：通过。涵盖字段/常量编辑、工作副本恢复、只读原始快照、原始/混淆后程序运行与取消、Trim 报告、SnakeGame、两种反编译器、字节码、手动配对、Unicode include、脚本失败、`execute`、取消、嵌套与 MR 归档导入、多次混淆、项目移动后反编译历史结果。
- `tests/test_launcher.py` 在 Java 17 / 21：通过。无参数启动、无浏览器回退、指定端口冲突、非法端口、HTTP 退出。
- 基础版已验证原 Swing 在独立 JVM 中启动（本轮未修改其入口）：进程保持运行，未发现启动异常；测试后关闭。
- CLI/Web 等价：同输入与非随机配置下，类名、成员声明、字节码指令及实际运行结果一致。原引擎两次纯 CLI 输出也会重排常量池，因此不要求原始 class 字节完全相同。
- JAR 检查：包含前端、Monaco worker、CFR、Vineflower、第三方许可；无重复 ZIP 条目。`git diff --check` 通过。

产物：`build/libs/zkm27.jar`，13,861,213 字节。
SHA-256：`5da7bcbae436ff9a6c958ede8922b66a71bd97bee84c6aba61c28023c9c53107`。

浏览器截图在 `build/playwright/`，新按钮见 `project-file-actions.png`。原生窗口完整交互尚未完成自动验收；macOS 实际选择器已返回取消，成功选择/另存为通过接口替身与文件导出测试验证。Windows / Linux 未在本机实测；已提供六组合跨平台 CI（Windows/macOS/Linux × Java 17/21）。CI 工作流尚未在远端运行。
