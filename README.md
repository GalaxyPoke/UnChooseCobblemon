# UnChooseCobblemon

一个用于禁用Cobblemon初始宝可梦选择的Bukkit/Spigot插件。

## 功能特性

- ✅ **初始宝可梦选择禁用** - 阻止玩家选择初始宝可梦
- ✅ **SQLite/MySQL支持** - 灵活的数据存储方案
- ✅ **自定义GUI界面** - 美观的管理界面
- ✅ **完整的命令系统** - 丰富的管理命令
- ✅ **权限系统** - 精细的权限控制
- ✅ **多语言支持** - 支持中文和英文
- ✅ **高性能设计** - 异步数据库操作和智能缓存
- ✅ **并发安全** - 支持多人同时使用

## 环境要求

- Minecraft 1.21.1
- Java 21+
- Spigot/Paper 或混合服务端 (Mohist/Arclight/Banner)
- Cobblemon Mod

## 安装

1. 下载最新版本的 `UnChooseCobblemon-x.x.x.jar`
2. 将jar文件放入服务器的 `plugins` 目录
3. 重启服务器
4. 编辑 `plugins/UnChooseCobblemon/config.yml` 进行配置
5. 使用 `/uc reload` 重载配置

## 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/uc help` | 显示帮助信息 | `unchoose.use` |
| `/uc gui` | 打开管理GUI | `unchoose.gui` |
| `/uc status [玩家]` | 查看玩家状态 | `unchoose.query` |
| `/uc lock <玩家>` | 锁定玩家 | `unchoose.setstarterlock` |
| `/uc unlock <玩家>` | 解锁玩家 | `unchoose.setstarterlock` |
| `/uc lockall` | 锁定所有在线玩家 | `unchoose.admin` |
| `/uc unlockall` | 解锁所有在线玩家 | `unchoose.admin` |
| `/uc reload` | 重载配置 | `unchoose.reload` |

## 权限

| 权限节点 | 描述 | 默认 |
|----------|------|------|
| `unchoose.use` | 使用基本命令 | true |
| `unchoose.gui` | 打开GUI界面 | true |
| `unchoose.bypass` | 绕过初始选择禁用 | op |
| `unchoose.admin` | 管理员权限 | op |
| `unchoose.reload` | 重载配置 | op |
| `unchoose.setstarterlock` | 设置玩家锁定状态 | op |
| `unchoose.query` | 查询玩家数据 | op |

## 配置文件

```yaml
# 数据库配置
database:
  type: sqlite  # sqlite 或 mysql
  
# 初始宝可梦禁用设置
starter:
  enabled: true
  mode: "notify"  # block / notify / log
  allow-bypass: true
  auto-lock-new-players: true
```

详细配置请参考 `config.yml` 文件中的注释。

## 编译

```bash
# 克隆项目
git clone <repository>

# 进入项目目录
cd UnChooseCobblemon/plugin

# 使用Gradle编译
./gradlew shadowJar

# 编译产物位于 build/libs/UnChooseCobblemon-x.x.x.jar
```

## 项目结构

```
plugin/
├── src/main/kotlin/com/unchoose/cobblemon/
│   ├── UnChooseCobblemon.kt      # 主插件类
│   ├── command/                   # 命令处理
│   ├── config/                    # 配置管理
│   ├── database/                  # 数据库操作
│   ├── gui/                       # GUI界面
│   ├── listener/                  # 事件监听
│   ├── manager/                   # 数据管理
│   ├── model/                     # 数据模型
│   └── util/                      # 工具类
├── src/main/resources/
│   ├── plugin.yml                 # 插件描述
│   ├── config.yml                 # 默认配置
│   └── lang/                      # 语言文件
└── build.gradle.kts               # Gradle构建脚本
```

## 注意事项

1. **混合服务端**: Cobblemon是Fabric/NeoForge模组，需要使用混合服务端(如Mohist, Arclight, Banner)才能与Bukkit插件共同工作。

2. **事件拦截**: 由于Cobblemon使用自己的事件系统，本插件通过反射和混合服务端API来拦截事件。具体效果可能因服务端实现而异。

3. **数据库选择**: 
   - 小型服务器推荐使用SQLite
   - 大型服务器或需要远程访问推荐使用MySQL

## 许可证

MIT License

## 支持

如有问题，请提交Issue或联系作者。
