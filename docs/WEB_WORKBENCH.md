# ZKM 27 本地 Web 工作台 / Local Web Workbench

## 启动

运行环境为 Java 17 或更高版本。发行文件只有 `build/libs/zkm27.jar`；浏览器页面、Monaco、CFR 0.152 和 Vineflower 1.12.0 均包含在 JAR 内，运行时不需要 Node.js 或联网。

```sh
java -jar build/libs/zkm27.jar
java -jar build/libs/zkm27.jar --web --port 8270 --no-browser
java -jar build/libs/zkm27.jar --swing
java -jar build/libs/zkm27.jar script.zkm
```

无参数默认打开 Web；原脚本参数仍交给原 CLI。服务绑定 `127.0.0.1`，默认随机空闲端口。端口被占用会明确报错。浏览器无法打开时使用终端中的完整链接（含 `#token=`）。工作台的“退出程序”停止所有 worker；关闭标签页不影响任务。

第一次语言跟随浏览器，主题跟随系统；右上角可切换，也可以在设置中恢复“跟随系统 / 浏览器”。Web 中的 Swing 按钮启动独立进程。原 `.ser` 文件格式保持不变。

## 推荐流程

1. 新建项目目录，引用本机 JAR、目录和依赖。浏览 / 选择目录按钮调用系统原生文件选择窗口（macOS 原生选择器、Windows 系统对话框、Linux zenity/kdialog），不再使用网页文件浏览器，输入框仍支持直接路径；拖入文件会复制到项目的 `imports/` 下。路径旁的定位按钮可替换丢失文件。
2. 在 Build Helper 配置 Trim、名称排除、包含规则、混淆参数和输出目录。没有设置的选项使用原引擎默认值。Trim 的测试报告复用原 `reportTrimPreview`，不修改活动项目。
3. 打开类浏览器，默认自动加载原始源码。选择“仅原始 / 混淆后 / 对比”；后两种视图自动试运行当前 Build Helper 配置。配置没变时复用已有或进行中的结果，改变后重新生成；失败或取消后显示重试按钮，不无限重跑。下拉框也可固定到一次正式构建、脚本或导入记录。
4. 类浏览器源码视图左侧默认显示可展开的包树，点击类直接反编译；搜索保留匹配类的包层级，支持键盘方向键与全部折叠，切换页面保留展开状态。混淆后视图按混淆后的包名组织，对比视图沿用原始包结构。可切换 CFR/Vineflower、单侧取消、调整超时、重试或查看字节码。Monaco 提供搜索、复制、差异导航和同步滚动。“成员与编辑”保留包树、继承关系、字段、方法及常量编辑。手动配对保存在所选运行记录中。Build Helper 的手动试运行与正式构建按钮也会打开这里；试运行输出位于 `runs/<id>/preview-output/`，正式构建写到配置的输出位置。
5. “作为脚本编辑”将向导结果加入脚本标签页。可以导入、新建、保存、另存为、校验和执行编辑缓冲区；无需先覆盖原脚本。脚本导入时默认使用脚本所在目录作为执行目录，项目页可修改。

6. “运行输出对比”分别启动两侧的真实 Java 程序。默认“最新成功构建（自动跟随）”：新构建完成或返回页面时刷新两侧路径，正在运行两侧程序时等本次运行结束后更新。选择历史记录或手动编辑类路径后固定使用该配置，可重新选择自动模式；运行参数、标准输入和已执行的输出保持不变。也可自行选择 JAR/类目录/依赖。留空主类时读取 JAR 的 `Main-Class`；两侧主类可不同。程序参数和 JVM 参数每行一个，保留空格、不经过 shell；共享执行目录与标准输入，发送输入后关闭 stdin。点击“运行两侧”，分别比较 stdout、stderr、退出码和耗时，支持单侧取消及超时。程序读取的是表单所示的本机文件，若历史产物后来被覆盖，可改用对应试运行目录中的产物。

7. 项目页及 Build Helper 的“输出”步骤提供“打开项目文件夹”“打开输出文件夹”和“导出成品 JAR”。文件夹由 Finder / Explorer / 系统文件管理器打开；输出优先采用最新成功构建或试运行的实际位置，没有记录时采用配置的输出位置。导出通过系统“另存为”窗口保存，已有 JAR 保持字节不变，纯类目录连同资源和现有 Manifest 打包；多个 JAR 先选择产物。取消窗口不写文件，失败任务不可导出。

Linux 的原生对话框需要系统安装 `zenity`（GTK）或 `kdialog`（KDE），打开文件夹需要 `xdg-open`；缺少时显示错误，可继续手动输入路径。macOS / Windows 使用系统自带组件。系统窗口在运行 JAR 的本机桌面打开。

切换导航保留当前项目已打开页面的步骤、选项卡、筛选、表单、编辑器和滚动位置；切换项目会隔离页面状态。配置和脚本仍保存到项目文件，临时页面位置保留在当前浏览器会话中。

**`execute` 保留原引擎行为，正式运行和试运行都会执行。** 输出重定向通过语句执行上下文完成，没有正则修改脚本文本。外部命令仍使用原命令、工作目录和文件操作语义。

## 项目与恢复

```text
project.zkm.json           # schemaVersion、revision、配置及脚本标签
scripts/                  # 保存的 ZKM 脚本
imports/                  # 拖入的副本
work/checkpoint-<job>/     # 最近成功编辑/变换后的可重开工作副本
runs/<job>/
  project.json            # 运行配置快照
  request.json            # 请求及源文件位置
  script.zkm              # 实际运行的脚本文本
  job.json, console.log   # 状态及原始日志
  before/, after/         # 字节码与类身份清单
  artifacts.json          # 输出文件和 ChangeLog
  manual-pairs.json       # 手动配对（如有）
  stdout.txt, stderr.txt  # programRun 的标准输出/错误输出（如有）
```

项目内路径保存为相对路径，外部文件保留绝对路径。源文件不因 Web 编辑而改变；编辑和交互式 Trim/Obfuscate 操作写入工作副本，后续向导构建默认使用该副本。“使用原始输入”放弃工作副本。失败或取消后重开 worker 时使用最近成功的副本。

最近项目和反编译缓存位于 `~/.zkm-workbench`，可用 `-Dzkm.web.home=/path` 修改。重启会重开最近的可用项目；中断任务标记为失败，不作为对比成功记录。更换设备时应重新定位外部输入和依赖；项目内的字节码快照和运行结果路径使用相对位置恢复。

类身份含来源归档、嵌套条目、multi-release 版本和类名。原生运行使用稳定输入身份关联改名；导入已有结果使用原 ChangeLog 解析器组合映射。没有可靠映射时只配对唯一同名类，其余显示“未映射 / 映射不唯一”，允许手动选择。随机混淆的不同运行各自保存结果。

## 构建与测试

源码构建需要 JDK 17+、Node.js 22+ 和 npm。首次构建需要下载锁定的依赖；依赖缓存齐全后可离线构建。离线运行成品 JAR 不需要这些构建工具。

```sh
sh gradlew test jar
npm --prefix web test
python3 tests/test_launcher.py
python3 tests/run_smoke.py
cd web
npx playwright install chromium
npm run test:e2e
```

Windows 可使用 `gradlew.bat test jar`。`jar` 自动生成包含所有依赖的 Shadow JAR，薄 JAR 标记为 `-thin`，发布时使用 `zkm27.jar`。原归档条目的时间戳处理和 service 描述合并保留。第三方许可位于 JAR 的 `META-INF/licenses/` 与 Web 设置中的许可链接。

原引擎即使设置 `randomize=false`，也可能在不同进程中重排常量池。CLI/Web 等价测试因此比较名称、声明、解析后的字节码指令和实际运行行为，不把常量池索引顺序视为语义差异。

JUnit 校验前端全部选项对应的引擎语法、预览路径映射、项目冲突、include 上下文、同名类歧义和缓存失效。浏览器测试覆盖中英、主题、配置、脚本、类浏览、真实混淆和双栏对比。打包冒烟测试覆盖 worker、常量/字段编辑、恢复、两种反编译器、字节码、`execute`、取消、Unicode include、嵌套/MR 归档、SnakeGame 和原 CLI。

跨平台 Java 17/21 检查配置在 `.github/workflows/web-workbench.yml`；CI 配置不代表已在当前机器实测其他操作系统。详见 [本次验证记录](WEB_VALIDATION.md)。

## 后端接口与隔离

`/api/v1` 使用 JSON DTO，启动令牌先换为 HttpOnly / SameSite=Strict 会话 cookie。所有 API 都检查凭据、Host 和 Origin；不监听 LAN。请从启动链接进入，避免打开裸地址导致未授权。

- `GET /state`，`POST /projects/create`、`/projects/open`，`GET/POST /project`
- `GET /files`、`/files/read`，`POST /files/write`、`/files/import`
- `POST /desktop/pick`、`/desktop/open`：原生文件/目录选择与打开文件夹
- `GET /outputs?jobId=<id>`、`POST /outputs/export`：成功产物列表与原生另存为导出
- `POST /build/script`、`/engine`，`GET/POST /jobs`、`GET /jobs/<id>`、`POST /jobs/<id>/cancel`
- `GET /events`：SSE，日志、任务状态、事件 ID 和断线重连
- `GET /runtime/config?jobId=<id>`：从记录取得程序运行路径
- `GET /comparison`，`POST /comparison/manual`，`GET /artifacts`、`/artifact`
- `POST /swing`、`/shutdown`

项目保存使用 `revision`，任务请求携带 `projectRevision`；过期版本返回 HTTP 409。HTTP 主进程只管理文件与任务，原引擎及反编译运行在子 JVM；活动仓库串行操作。脚本、试运行、导入和反编译任务各自隔离。默认每侧反编译超时 60 秒，可在 UI 中改为 1–600 秒。缓存键包含所有上下文类、依赖文件内容和固定引擎版本。

`inspect` 任务只打开类并生成反编译快照，不执行 Trim、混淆和保存。`programRun` 任务在独立子 JVM 运行程序；非零退出码与超时保留为可对比结果。每侧默认超时 60 秒（1–3600 秒可调），stdout/stderr 各保留最多 2,097,152 个字符并明确标记截断；取消时终止 worker 及其子进程。运行记录包含实际命令、输入参数及结果，刷新不影响任务。
