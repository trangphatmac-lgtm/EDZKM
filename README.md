# Open ZelixKlassMaster™ 27

<p align="center">
  <img src="./img/zkmrip.png" alt="2" width="100%">
</p>
<p align="center">
  <img src="./img/follow.jpg" alt="2" width="20%" />
  <img src="./img/deobf3.jpg" alt="3" width="20%" />
  <img src="./img/heavy.jpg" alt="4" width="20%" />
</p>

### Join Discord：https://discord.gg/TPjtZt75Vs

youtube：【ZelixKlassMaster27 fully deobf+idea ready】 https://youtu.be/GhLJreiEuCU

bilibili：【最新最热 zkm 27 原始码泄露视频 开源去验证可直接构建】 https://www.bilibili.com/video/BV1Eyhd6zE1Y/

## requirements

- Java 17 or newer to run the packaged application
- JDK 17+, Node.js 22+ and npm to build from source

## modify

full offline, deobf, rename, fix, patch auth, and more....

## build

```
sh gradlew jar
```

output: `build/libs/zkm27.jar`

## runnnnnnnnnn

Web UI (default; opens the browser automatically):

```
sh gradlew run
```

or directly:

```
java -jar build/libs/zkm27.jar
```

Original Swing interface:

```sh
java -jar build/libs/zkm27.jar --swing
```

Use `--web --port 8270 --no-browser` for a fixed local port without opening the browser.
The single JAR includes the React/Monaco frontend and CFR 0.152 / Vineflower 1.12.0; runtime is fully offline.

[Web workbench guide / 中文使用说明](docs/WEB_WORKBENCH.md) · [Swing feature migration checklist](docs/WEB_FEATURES.md)

Script mode:

```
java -jar build/libs/zkm27.jar script.zkm
```

## IntelliJ IDEA

1. open the project folder, wait for the gradle sync.
2. set the project SDK to JDK 17+.
3. select the **start** run configuration in `.idea/runConfigurations/` and hit run.

## join us

> this source code was generated via decompilation and may contain minor behavioral discrepancies or bugs. Although we have worked hard to fix them, 100% accuracy cannot be guaranteed.
>
> Contributions and bug reports are welcome—please feel free to open an **Issue** or submit a **PR**!

## thx

- **original obfuscated software:** Zelix KlassMaster 27.0.0
- **some deobfuscation, symbol recovery, rename, mapping:** completed by Claude™ and Codex™ under human supervision
- **flowdeobf, indydeobf, deobf tools:** NoHackClient™
- **Java Deobfuscator**
- **Recaf**

## legal & disclaimers

- **Affiliation:** This project is an independent community development and is **NOT AN OFFICIAL ZELIX PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH ZELIX PTY LTD.**
- **Assets & Code:** No proprietary assets or un-obfuscated original source code are distributed directly within this repository. (All third-party assets and code belong to their respective owners).

