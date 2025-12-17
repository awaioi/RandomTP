package org.awaioi.randomtp;

import org.awaioi.randomtp.commands.RTPCommand;
import org.awaioi.randomtp.config.ConfigManager;
import org.awaioi.randomtp.data.PlayerDataManager;
import org.awaioi.randomtp.economy.EconomyManager;
import org.awaioi.randomtp.economy.EconomySystemManager;
import org.awaioi.randomtp.listeners.PlayerListener;
import org.awaioi.randomtp.teleport.TeleportManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 随机传送插件主类
 * 提供完整的随机传送功能，包括经济系统、冷却机制、权限管理等
 */
public class RandomTP extends JavaPlugin {
    
    private static RandomTP instance;
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private EconomyManager economyManager;
    private EconomySystemManager economySystemManager;
    private TeleportManager teleportManager;
    private RTPCommand rtpCommand;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        if (!configManager.loadConfig()) {
            getLogger().severe("配置文件加载失败！插件将在3秒后禁用...");
            getServer().getScheduler().runTaskLater(this, () -> {
                getServer().getPluginManager().disablePlugin(this);
            }, 60L);
            return;
        }
        
        // 初始化经济管理器
        economyManager = new EconomyManager(this);
        economyManager.initialize();
        
        // 初始化经济系统管理器
        economySystemManager = new EconomySystemManager(this);
        economySystemManager.initialize();
        
        // 初始化玩家数据管理器
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.loadPlayerData();
        
        // 初始化传送管理器
        teleportManager = new TeleportManager(this);
        
        // 注册命令
        rtpCommand = new RTPCommand(this);
        if (!rtpCommand.register()) {
            getLogger().severe("命令注册失败！插件将在3秒后禁用...");
            getServer().getScheduler().runTaskLater(this, () -> {
                getServer().getPluginManager().disablePlugin(this);
            }, 60L);
            return;
        }
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().info("随机传送插件已成功启用！");
    }
    
    @Override
    public void onDisable() {
        // 保存所有玩家数据
        if (playerDataManager != null) {
            playerDataManager.savePlayerData();
        }
        
        // 取消所有传送任务
        if (teleportManager != null) {
            teleportManager.cancelAllTeleports();
        }
        
        // 关闭经济系统管理器
        if (economySystemManager != null) {
            economySystemManager.shutdown();
        }
        
        getLogger().info("随机传送插件已禁用");
    }
    
    /**
     * 获取插件实例
     */
    public static RandomTP getInstance() {
        return instance;
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取玩家数据管理器
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    /**
     * 获取经济管理器
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    /**
     * 获取经济系统管理器
     */
    public EconomySystemManager getEconomySystemManager() {
        return economySystemManager;
    }
    
    /**
     * 获取传送管理器
     */
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}