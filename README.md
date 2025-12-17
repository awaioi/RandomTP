# RandomTP - 智能随机传送插件

[![Version](https://img.shields.io/badge/Version-1.5.0-brightgreen.svg)](https://github.com/awaioi/RandomTP)
[![API](https://img.shields.io/badge/API-Paper%201.19+-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)

一个功能完整的 Minecraft 随机传送插件，支持经济系统、冷却机制、权限管理、智能检测和数据优化。专为 Paper/Spigot 服务器设计，提供安全可靠的随机传送体验。

## ✨ 核心特性

### 🚀 智能传送系统
- **安全位置检测**: 多阶段智能检测算法，确保传送位置安全
- **范围控制**: 可配置传送范围，支持动态调整
- **高度限制**: 支持 Y 轴高度限制配置
- **环境避让**: 智能避开水源、岩浆等危险环境

### 💰 完整经济系统
- **多级费用**: 支持普通/VIP/VIP+ 用户不同费用设置
- **经济适配**: 兼容 Vault、EssentialsX 等主流经济插件
- **货币格式化**: 自动适配不同服务器货币格式
- **交易日志**: 完整的交易记录和统计功能

### ⏱️ 智能冷却机制
- **分级冷却**: 不同用户组拥有不同冷却时间
- **时间计算**: 精确的冷却时间倒计时显示
- **权限绕过**: 管理员可绕过任意玩家冷却

### 🛡️ 权限管理系统
- **精细权限**: 详细的权限节点控制
- **用户组支持**: VIP/VIP+ 用户享受优惠
- **管理员权限**: 完整的管理员控制权限

### 📊 数据优化系统
- **分层存储**: 活跃/归档数据分离管理
- **自动清理**: 智能数据过期清理机制
- **日志轮转**: 多维度日志轮转和归档
- **性能监控**: 实时性能统计和监控

### 🎨 视觉效果
- **传送特效**: 完整的传送开始/过程/到达特效
- **无敌保护**: 传送后短暂无敌时间
- **消息提示**: 丰富的状态提示和错误处理

## 📋 系统要求

- **Minecraft 版本**: 1.19+ (Paper/Spigot)
- **Java 版本**: Java 8 或更高版本
- **依赖插件**: Vault (可选，用于经济系统)
- **推荐插件**: EssentialsX (用于经济适配)

## 🔧 安装说明

### 1. 下载插件
从 [Releases](https://github.com/awaioi/RandomTP/releases) 下载最新版本的 JAR 文件。

### 2. 安装插件
将 `RandomTP-1.5.0.jar` 文件放入服务器的 `plugins` 目录中。

### 3. 重启服务器
重启服务器或使用 `/reload` 命令加载插件。

### 4. 基础配置
插件首次运行会自动生成默认配置文件 `config.yml`。

## ⚙️ 配置说明

### 基础配置示例
```yaml
# 传送设置
teleport:
  range: 10000          # 传送范围
  max-tries: 10         # 最大尝试次数
  min-y: 0             # 最小Y坐标
  max-y: 256           # 最大Y坐标
  find-safe-location: true    # 寻找安全位置
  avoid-water: true          # 避免水源
  avoid-lava: true           # 避免岩浆

# 冷却时间设置（秒）
cooldown:
  default: 30          # 默认冷却
  vip: 20              # VIP冷却
  vipplus: 10          # VIP+冷却

# 经济设置
economy:
  enabled: true        # 启用经济系统
  cost:
    default: 100.0     # 默认费用
    vip: 50.0          # VIP费用
    vipplus: 10.0      # VIP+费用

# 消息设置
messages:
  prefix: "[RTP]"
  success: "&a传送成功！"
  cooldown: "&c请等待 {time} 秒后再试"
  no-money: "&c金钱不足，无法传送"
```

详细配置选项请参考 [`docs/config_example.yml`](docs/config_example.yml)

## 🎮 使用方法

### 基础命令
```bash
/rtp              # 执行随机传送
/rtp help         # 显示帮助信息
/rtp info         # 查看传送状态信息
```

### 管理员命令
```bash
/rtp admin <玩家>     # 免费传送指定玩家
/rtp bypass <玩家>    # 绕过指定玩家冷却
/rtp reload          # 重载配置文件
/rtp setcost <类型> <费用>  # 设置传送费用
```

### 命令参数说明
- `<玩家>`: 目标玩家名称
- `<类型>`: 费用类型 (default/vip/vipplus)
- `<费用>`: 传送费用金额

## 🔐 权限节点

### 用户权限
- `rtp.use` - 基础随机传送权限
- `rtp.info` - 查看传送信息权限

### VIP 权限
- `rtp.vip` - VIP用户权限（更低费用和冷却）
- `rtp.vipplus` - VIP+用户权限（最低费用和冷却）

### 管理员权限
- `rtp.admin` - 完整管理员权限
- `rtp.reload` - 重载配置权限
- `rtp.setcost` - 设置费用权限
- `rtp.free` - 免费传送权限
- `rtp.bypass` - 绕过冷却权限

## 🛠️ 开发构建

### 环境要求
- Java 8+
- Maven 3.6+

### 编译步骤
```bash
# 克隆项目
git clone https://github.com/awaioi/RandomTP.git
cd RandomTP

# 编译项目
mvn clean package

# 编译结果
# 生成文件: target/RandomTP-1.5.0.jar
```

### 开发依赖
- Paper API 1.19.4-R0.1-SNAPSHOT
- Vault API 1.7
- Slf4j API 1.7.36

## 📚 文档资源

- 📖 [配置示例](docs/config_example.yml) - 详细配置说明
- 💰 [经济系统文档](docs/ECONOMY_SYSTEM_README.md) - 经济系统完整说明
- 🔄 [数据优化方案](docs/DATA_OPTIMIZATION_SOLUTION.md) - 性能优化策略
- 📋 [兼容性测试](docs/COMPATIBILITY_TEST_PLAN.md) - 插件兼容性测试
- 🔀 [升级指南](docs/MIGRATION_GUIDE_v1.3.0.md) - 版本升级指南

## 🎯 主要功能

| 功能模块 | 状态 | 描述 |
|---------|------|------|
| 🌍 随机传送 | ✅ | 智能安全位置检测和传送 |
| 💰 经济系统 | ✅ | 多级费用和交易管理 |
| ⏱️ 冷却机制 | ✅ | 分级冷却和时间控制 |
| 🛡️ 权限管理 | ✅ | 精细权限和用户组支持 |
| 📊 数据优化 | ✅ | 分层存储和自动清理 |
| 🎨 视觉效果 | ✅ | 传送特效和无敌保护 |
| 📝 日志系统 | ✅ | 完整日志记录和轮转 |
| 🔧 管理功能 | ✅ | 丰富的管理命令 |

## 🔄 更新日志

### v1.5.0 (当前版本)
- ✨ 新增传送成功消息显示花费功能
- 🔧 修复经济系统集成问题
- 📊 增强数据优化管理
- 🐛 修复多项编译和运行问题

### v1.3.0
- 💰 完整经济系统实现
- 📊 数据优化和日志管理
- 🎨 视觉效果系统
- 🛡️ 权限管理系统

### v1.2.0
- ⏱️ 智能冷却机制
- 🔍 优化安全检测算法
- 📝 完善错误处理

### v1.1.0
- 🌍 基础随机传送功能
- ⚙️ 配置系统
- 📱 基础命令系统

## 🤝 贡献指南

欢迎提交 Issues 和 Pull Requests！

### 贡献流程
1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👥 团队

- **开发者**: awaioi
- **版本**: 1.5.0

## 🆘 支持与反馈

- 🐛 **Bug 反馈**: [GitHub Issues](https://github.com/awaioi/RandomTP/issues)
- 💡 **功能建议**: [GitHub Discussions](https://github.com/awaioi/RandomTP/discussions)
- 📧 **邮箱联系**: support@awaioi.org

## 🙏 致谢

感谢以下开源项目和社区的支持：
- [PaperMC](https://papermc.io/) - 提供优秀的 Minecraft 服务器软件
- [Vault](https://github.com/MilkBowl/Vault) - 经济系统 API
- [Spigot](https://www.spigotmc.org/) - Minecraft 插件开发平台

---
