package org.awaioi.randomtp.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.awaioi.randomtp.RandomTP;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 配置文件管理器
 * 负责加载、保存和管理插件配置文件
 */
public class ConfigManager {
    
    private final RandomTP plugin;
    private File configFile;
    private FileConfiguration config;
    
    public ConfigManager(RandomTP plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * 加载配置文件
     */
    public boolean loadConfig() {
        if (!configFile.exists()) {
            plugin.getLogger().info("配置文件不存在，正在创建默认配置文件...");
            saveDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 设置默认值
        setDefaults();
        
        // 保存配置
        saveConfig();
        
        return true;
    }
    
    /**
     * 设置默认配置值
     */
    private void setDefaults() {
        // 传送设置
        config.addDefault("teleport.range", 1000);
        config.addDefault("teleport.delay", 3);
        config.addDefault("teleport.cooldown", 300);
        
        // 经济设置
        config.addDefault("economy.enabled", true);
        config.addDefault("economy.cost.default", 100);
        config.addDefault("economy.cost.vip", 80);
        config.addDefault("economy.cost.vipplus", 50);
        config.addDefault("economy.refund.enabled", true);
        config.addDefault("economy.refund.on-move", true);
        config.addDefault("economy.refund.on-death", true);
        config.addDefault("economy.refund.on-teleport", true);
        config.addDefault("economy.system-detection.auto", true);
        config.addDefault("economy.fallback-to-vault", true);
        config.addDefault("economy.debug-mode", false);
        
        // 安全传送设置
        config.addDefault("safety.min-y", 64);
        config.addDefault("safety.max-y", 200);
        config.addDefault("safety.avoid-water", true);
        config.addDefault("safety.avoid-lava", true);
        config.addDefault("safety.find-safe-location", true);
        config.addDefault("safety.max-tries", 10);
        
        // 消息设置
        config.addDefault("messages.prefix", "&8[&6RTP&8] &r");
        config.addDefault("messages.teleporting", "&a正在准备随机传送...");
        config.addDefault("messages.teleported", "&a传送成功！");
        config.addDefault("messages.insufficient-funds", "&c硬币不足！需要 %cost% 硬币");
        config.addDefault("messages.cooldown", "&c还需等待 %time% 秒才能再次传送");
        config.addDefault("messages.no-permission", "&c你没有权限使用此命令");
        config.addDefault("messages.reload-success", "&a配置文件已重载");
        config.addDefault("messages.reload-failed", "&c配置文件重载失败");
        config.addDefault("messages.cost-set", "&a传送费用已设置为 %cost% 硬币");
        config.addDefault("messages.teleport-cancelled", "&c传送已取消");
        config.addDefault("messages.admin-teleport", "&a已为玩家 %player% 执行随机传送");
        config.addDefault("messages.bypass-cooldown", "&a已为玩家 %player% 绕过冷却");
        config.addDefault("messages.info-header", "&8&m---&r &6随机传送信息 &8&m---");
        config.addDefault("messages.info-cost", "&a基础费用: &f%cost% 硬币");
        config.addDefault("messages.info-cooldown", "&a冷却时间: &f%cooldown% 秒");
        config.addDefault("messages.info-range", "&a传送范围: &f%range% 格");
        
        // 冷却时间设置（按权限）
        config.addDefault("cooldowns.default", 300);
        config.addDefault("cooldowns.vip", 180);
        config.addDefault("cooldowns.vipplus", 120);
        
        // 权限设置
        config.addDefault("permissions.rtp.use", "rtp.use");
        config.addDefault("permissions.rtp.vip", "rtp.vip");
        config.addDefault("permissions.rtp.vipplus", "rtp.vipplus");
        config.addDefault("permissions.rtp.admin", "rtp.admin");
        config.addDefault("permissions.rtp.bypass", "rtp.bypass");
        config.addDefault("permissions.rtp.free", "rtp.free");
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存配置文件: " + e.getMessage());
        }
    }
    
    /**
     * 重载配置文件
     */
    public boolean reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream)
            );
            config.setDefaults(defaultConfig);
        }
        
        setDefaults();
        saveConfig();
        
        return true;
    }
    
    /**
     * 获取配置对象
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 保存默认配置
     */
    public void saveDefaultConfig() {
        plugin.saveResource("config.yml", false);
    }
    
    // 配置获取方法
    public int getTeleportRange() {
        return config.getInt("teleport.range");
    }
    
    public int getTeleportDelay() {
        return config.getInt("teleport.delay");
    }
    
    public int getCooldownTime() {
        return config.getInt("teleport.cooldown");
    }
    
    public boolean isEconomyEnabled() {
        return config.getBoolean("economy.enabled");
    }
    
    public boolean isEconomyRefundEnabled() {
        return config.getBoolean("economy.refund.enabled");
    }
    
    public boolean shouldRefundOnMove() {
        return config.getBoolean("economy.refund.on-move");
    }
    
    public boolean shouldRefundOnDeath() {
        return config.getBoolean("economy.refund.on-death");
    }
    
    public boolean shouldRefundOnTeleport() {
        return config.getBoolean("economy.refund.on-teleport");
    }
    
    public boolean isAutoEconomyDetection() {
        return config.getBoolean("economy.system-detection.auto");
    }
    
    public boolean shouldFallbackToVault() {
        return config.getBoolean("economy.fallback-to-vault");
    }
    
    public boolean isEconomyDebugMode() {
        return config.getBoolean("economy.debug-mode");
    }
    
    public double getTeleportCost(String permission) {
        String costKey = "economy.cost." + getPermissionSuffix(permission);
        return config.getDouble(costKey, config.getDouble("economy.cost.default"));
    }
    
    public double getTeleportCost(org.bukkit.entity.Player player) {
        // 获取玩家当前权限 - 检查玩家是否有特定权限
        if (player.hasPermission("rtp.vipplus")) {
            return getTeleportCost("vipplus");
        } else if (player.hasPermission("rtp.vip")) {
            return getTeleportCost("vip");
        }
        return config.getDouble("economy.cost.default");
    }
    
    private String getPermissionSuffix(String permission) {
        if (permission.contains("vipplus")) return "vipplus";
        if (permission.contains("vip")) return "vip";
        return "default";
    }
    
    public int getMinY() {
        return config.getInt("safety.min-y");
    }
    
    public int getMaxY() {
        return config.getInt("safety.max-y");
    }
    
    public boolean shouldAvoidWater() {
        return config.getBoolean("safety.avoid-water");
    }
    
    public boolean shouldAvoidLava() {
        return config.getBoolean("safety.avoid-lava");
    }
    
    public boolean shouldFindSafeLocation() {
        return config.getBoolean("safety.find-safe-location");
    }
    
    public int getMaxTries() {
        return config.getInt("safety.max-tries");
    }
    
    public String getMessage(String key) {
        String message = config.getString("messages." + key, "&c消息未找到: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getFormattedMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
        }
        return message;
    }
    
    public int getPlayerCooldown(String permission) {
        String cooldownKey = "cooldowns." + getPermissionSuffix(permission);
        return config.getInt(cooldownKey, config.getInt("teleport.cooldown"));
    }
    
    public String getPermission(String key) {
        return config.getString("permissions." + key, "");
    }
}