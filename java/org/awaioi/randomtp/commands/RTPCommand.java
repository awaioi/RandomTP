package org.awaioi.randomtp.commands;

import java.util.ArrayList;
import java.util.List;

import org.awaioi.randomtp.RandomTP;
import org.awaioi.randomtp.config.ConfigManager;
import org.awaioi.randomtp.economy.EconomySystemManager;
import org.awaioi.randomtp.teleport.TeleportManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * 随机传送命令处理器
 * 处理 /rtp 和 /wild 命令及其子命令
 */
public class RTPCommand implements TabExecutor {
    
    private final RandomTP plugin;
    private final ConfigManager configManager;
    private final TeleportManager teleportManager;
    private final EconomySystemManager economySystemManager;
    
    public RTPCommand(RandomTP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.teleportManager = plugin.getTeleportManager();
        this.economySystemManager = plugin.getEconomySystemManager();
    }
    
    /**
     * 注册命令
     */
    public boolean register() {
        // 注册 rtp 命令
        PluginCommand rtpCommand = plugin.getCommand("rtp");
        if (rtpCommand != null) {
            rtpCommand.setExecutor(this);
            rtpCommand.setTabCompleter(this);
        } else {
            plugin.getLogger().severe("无法获取 rtp 命令");
            return false;
        }
        
        // 注册 wild 命令（别名）
        PluginCommand wildCommand = plugin.getCommand("wild");
        if (wildCommand != null) {
            wildCommand.setExecutor(this);
            wildCommand.setTabCompleter(this);
        }
        
        return true;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rtp") || command.getName().equalsIgnoreCase("wild")) {
            return handleRTPCommand(sender, args);
        }
        return false;
    }
    
    /**
     * 处理RTP命令
     */
    private boolean handleRTPCommand(CommandSender sender, String[] args) {
        // 检查是否有参数
        if (args.length == 0) {
            // 基础传送命令
            return handleBasicTeleport(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                return handleHelpCommand(sender);
            case "info":
                return handleInfoCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "admin":
                return handleAdminCommand(sender, args);
            case "bypass":
                return handleBypassCommand(sender, args);
            case "setcost":
                return handleSetCostCommand(sender, args);
            case "economystatus":
                return handleEconomyStatusCommand(sender);
            default:
                sender.sendMessage(configManager.getMessage("prefix") + 
                    ChatColor.RED + "未知命令。使用 /rtp help 查看可用命令。");
                return true;
        }
    }
    
    /**
     * 处理基础传送命令
     */
    private boolean handleBasicTeleport(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.RED + "只有玩家才能使用此命令。");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查权限
        if (!player.hasPermission("rtp.use")) {
            player.sendMessage(configManager.getMessage("prefix") + 
                configManager.getMessage("no-permission"));
            return true;
        }
        
        // 检查玩家是否已有传送任务
        if (teleportManager.hasActiveTeleport(player.getUniqueId())) {
            player.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.YELLOW + "你已有传送任务正在进行中。");
            return true;
        }
        
        // 执行传送
        teleportManager.performTeleport(player, false, false);
        return true;
    }
    
    /**
     * 处理帮助命令
     */
    private boolean handleHelpCommand(CommandSender sender) {
        sender.sendMessage(configManager.getMessage("prefix") + 
            ChatColor.GOLD + "=== 随机传送帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/rtp " + ChatColor.WHITE + "- 随机传送到安全位置");
        sender.sendMessage(ChatColor.YELLOW + "/rtp info " + ChatColor.WHITE + "- 查看传送信息");
        
        if (sender.hasPermission("rtp.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/rtp admin <玩家> " + ChatColor.WHITE + "- 免费传送指定玩家");
            sender.sendMessage(ChatColor.YELLOW + "/rtp bypass <玩家> " + ChatColor.WHITE + "- 绕过玩家冷却");
            sender.sendMessage(ChatColor.YELLOW + "/rtp reload " + ChatColor.WHITE + "- 重载配置文件");
            sender.sendMessage(ChatColor.YELLOW + "/rtp setcost <类型> <费用> " + ChatColor.WHITE + "- 设置传送费用");
        }
        
        // 显示费用信息
        if (economySystemManager.isEnabled()) {
            sender.sendMessage(ChatColor.GOLD + "=== 传送费用 ===");
            sender.sendMessage(ChatColor.YELLOW + "普通玩家: " + ChatColor.WHITE + 
                economySystemManager.formatMoney(economySystemManager.getTeleportCost(playerIfExists(sender))));
            sender.sendMessage(ChatColor.YELLOW + "VIP: " + ChatColor.WHITE + 
                economySystemManager.formatMoney(economySystemManager.getTeleportCost(playerIfExists(sender))));
        }
        
        // 显示经济系统调试命令
        if (sender.hasPermission("rtp.admin")) {
            sender.sendMessage(ChatColor.GOLD + "/rtp economystatus " + ChatColor.WHITE + "- 查看经济系统状态");
        }
        
        return true;
    }
    
    /**
     * 处理信息命令
     */
    private boolean handleInfoCommand(CommandSender sender) {
        Player player = playerIfExists(sender);
        
        sender.sendMessage(configManager.getMessage("prefix") + 
            ChatColor.stripColor(configManager.getMessage("info-header")));
        sender.sendMessage(configManager.getFormattedMessage("info-cost", 
            "cost", economySystemManager.formatMoney(economySystemManager.getTeleportCost(player))));
        sender.sendMessage(configManager.getFormattedMessage("info-cooldown", 
            "cooldown", String.valueOf(configManager.getPlayerCooldown(getPlayerPermission(player)))));
        sender.sendMessage(configManager.getFormattedMessage("info-range", 
            "range", String.valueOf(configManager.getTeleportRange())));
        
        // 如果是玩家，显示冷却信息
        if (player != null) {
            long remainingCooldown = plugin.getPlayerDataManager().getRemainingCooldown(player.getUniqueId());
            if (remainingCooldown > 0) {
                sender.sendMessage(ChatColor.YELLOW + "剩余冷却时间: " + ChatColor.WHITE + remainingCooldown + " 秒");
            } else {
                sender.sendMessage(ChatColor.GREEN + "你现在可以使用随机传送！");
            }
        }
        
        return true;
    }
    
    /**
     * 处理重载命令
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("rtp.admin")) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                configManager.getMessage("no-permission"));
            return true;
        }
        
        try {
            configManager.reloadConfig();
            plugin.getPlayerDataManager().loadPlayerData();
            sender.sendMessage(configManager.getMessage("prefix") + 
                configManager.getMessage("reload-success"));
        } catch (Exception e) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                configManager.getMessage("reload-failed"));
            plugin.getLogger().severe("重载配置文件失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理管理员传送命令
     */
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rtp.admin")) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.RED + "用法: /rtp admin <玩家>");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.RED + "玩家不在线或不存在。");
            return true;
        }
        
        // 执行免费传送
        teleportManager.freeTeleport(target);
        sender.sendMessage(configManager.getFormattedMessage("admin-teleport", 
            "player", target.getName()));
        
        return true;
    }
    
    /**
     * 处理绕过冷却命令
     */
    private boolean handleBypassCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rtp.bypass")) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.RED + "用法: /rtp bypass <玩家>");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.RED + "玩家不在线或不存在。");
            return true;
        }
        
        // 绕过冷却并传送
        teleportManager.bypassCooldownTeleport(target);
        sender.sendMessage(configManager.getFormattedMessage("bypass-cooldown", 
            "player", target.getName()));
        
        return true;
    }
    
    /**
     * 处理设置费用命令
     */
    private boolean handleSetCostCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rtp.admin")) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.RED + "用法: /rtp setcost <类型> <费用>");
            sender.sendMessage(ChatColor.GRAY + "类型: default, vip, vipplus");
            return true;
        }
        
        String type = args[1].toLowerCase();
        double cost;
        
        try {
            cost = Double.parseDouble(args[2]);
            if (cost < 0) {
                sender.sendMessage(configManager.getMessage("prefix") + 
                    ChatColor.RED + "费用不能为负数。");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.RED + "无效的费用数额。");
            return true;
        }
        
        // 设置费用
        String configPath = "economy.cost." + type;
        if (!configManager.getConfig().contains(configPath)) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                ChatColor.RED + "未知的费用类型: " + type);
            sender.sendMessage(ChatColor.GRAY + "可用类型: default, vip, vipplus");
            return true;
        }
        
        configManager.getConfig().set(configPath, cost);
        configManager.saveConfig();
        
        sender.sendMessage(configManager.getFormattedMessage("cost-set", 
            "cost", economySystemManager.formatMoney(cost)));
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 子命令补全
            List<String> subCommands = new ArrayList<>();
            subCommands.add("help");
            subCommands.add("info");
            
            if (sender.hasPermission("rtp.admin")) {
                subCommands.add("reload");
                subCommands.add("admin");
                subCommands.add("bypass");
                subCommands.add("setcost");
                subCommands.add("economystatus");
            }
            
            String prefix = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(prefix)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ((subCommand.equals("admin") || subCommand.equals("bypass")) && 
                sender.hasPermission("rtp.admin")) {
                // 玩家名补全
                String prefix = args[1].toLowerCase();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(player.getName());
                    }
                }
            } else if (subCommand.equals("setcost") && sender.hasPermission("rtp.admin")) {
                // 费用类型补全
                String prefix = args[1].toLowerCase();
                String[] types = {"default", "vip", "vipplus"};
                for (String type : types) {
                    if (type.startsWith(prefix)) {
                        completions.add(type);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setcost") && 
                   sender.hasPermission("rtp.admin")) {
            // 费用数值补全
            String prefix = args[2];
            if (prefix.matches("\\d*")) {
                completions.add("50");
                completions.add("100");
                completions.add("200");
            }
        }
        
        return completions;
    }
    
    /**
     * 获取玩家权限等级
     */
    private String getPlayerPermission(Player player) {
        if (player == null) return "rtp.use";
        
        if (player.hasPermission("rtp.vipplus")) {
            return "rtp.vipplus";
        } else if (player.hasPermission("rtp.vip")) {
            return "rtp.vip";
        } else {
            return "rtp.use";
        }
    }
    
    /**
     * 处理经济系统状态命令
     */
    private boolean handleEconomyStatusCommand(CommandSender sender) {
        if (!sender.hasPermission("rtp.admin")) {
            sender.sendMessage(configManager.getMessage("prefix") + 
                configManager.getMessage("no-permission"));
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== 经济系统状态 ===");
        
        // 基本状态
        sender.sendMessage(ChatColor.YELLOW + "启用状态: " + 
            (economySystemManager.isEnabled() ? ChatColor.GREEN + "已启用" : ChatColor.RED + "已禁用"));
        
        // 当前适配器
        sender.sendMessage(ChatColor.YELLOW + "当前适配器: " + ChatColor.WHITE + 
            economySystemManager.getCurrentAdapterName());
        
        // 适配器详情
        sender.sendMessage(ChatColor.GOLD + "=== 适配器详情 ===");
        sender.sendMessage(ChatColor.YELLOW + "Vault适配器: " + 
            (economySystemManager.isAdapterAvailable("vault") ? 
                ChatColor.GREEN + "可用" : ChatColor.RED + "不可用"));
        sender.sendMessage(ChatColor.YELLOW + "EssentialsX适配器: " + 
            (economySystemManager.isAdapterAvailable("essentialsx") ? 
                ChatColor.GREEN + "可用" : ChatColor.RED + "不可用"));
        
        // 费用设置
        sender.sendMessage(ChatColor.GOLD + "=== 当前费用设置 ===");
        sender.sendMessage(ChatColor.YELLOW + "默认费用: " + ChatColor.WHITE + 
            economySystemManager.formatMoney(configManager.getConfig().getDouble("economy.cost.default", 100.0)));
        sender.sendMessage(ChatColor.YELLOW + "VIP费用: " + ChatColor.WHITE + 
            economySystemManager.formatMoney(configManager.getConfig().getDouble("economy.cost.vip", 50.0)));
        sender.sendMessage(ChatColor.YELLOW + "VIP+费用: " + ChatColor.WHITE + 
            economySystemManager.formatMoney(configManager.getConfig().getDouble("economy.cost.vipplus", 10.0)));
        
        // 退款设置
        sender.sendMessage(ChatColor.GOLD + "=== 退款设置 ===");
        boolean refundEnabled = configManager.getConfig().getBoolean("economy.refund.enabled", true);
        sender.sendMessage(ChatColor.YELLOW + "退款启用: " + 
            (refundEnabled ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));
        
        if (refundEnabled) {
            double refundPercent = configManager.getConfig().getDouble("economy.refund.percentage", 0.5);
            sender.sendMessage(ChatColor.YELLOW + "退款比例: " + ChatColor.WHITE + 
                (refundPercent * 100) + "%");
        }
        
        // 功能支持
        sender.sendMessage(ChatColor.GOLD + "=== 功能支持 ===");
        sender.sendMessage(ChatColor.YELLOW + "货币格式化: " + 
            (economySystemManager.hasFeature("CURRENCY_FORMATTING") ? 
                ChatColor.GREEN + "支持" : ChatColor.RED + "不支持"));
        sender.sendMessage(ChatColor.YELLOW + "批量操作: " + 
            (economySystemManager.hasFeature("BULK_OPERATIONS") ? 
                ChatColor.GREEN + "支持" : ChatColor.RED + "不支持"));
        sender.sendMessage(ChatColor.YELLOW + "交易记录: " + 
            (economySystemManager.hasFeature("TRANSACTION_LOGGING") ? 
                ChatColor.GREEN + "支持" : ChatColor.RED + "不支持"));
        
        sender.sendMessage(ChatColor.GRAY + "=== 调试信息 ===");
        sender.sendMessage(ChatColor.GRAY + "经济系统管理器已初始化并正常运行");
        
        return true;
    }
    
    /**
     * 如果发送者是玩家则返回Player对象，否则返回null
     */
    private Player playerIfExists(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }
}