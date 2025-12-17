package org.awaioi.randomtp.data;

import org.awaioi.randomtp.RandomTP;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家数据管理器
 * 负责管理玩家的传送数据、冷却时间等信息
 */
public class PlayerDataManager {
    
    private final RandomTP plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    public PlayerDataManager(RandomTP plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadPlayerData();
    }
    
    /**
     * 加载玩家数据
     */
    public void loadPlayerData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("玩家数据文件不存在，将创建新文件");
            savePlayerData();
            return;
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // 从配置中加载玩家数据
        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerData playerData = new PlayerData(uuid);
                playerData.setLastTeleport(dataConfig.getLong(uuidStr + ".lastTeleport"));
                playerData.setTeleportCount(dataConfig.getInt(uuidStr + ".teleportCount"));
                playerData.setTotalCost(dataConfig.getDouble(uuidStr + ".totalCost", 0.0));
                playerDataMap.put(uuid, playerData);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的UUID格式: " + uuidStr);
            }
        }
        
        plugin.getLogger().info("已加载 " + playerDataMap.size() + " 个玩家的数据");
    }
    
    /**
     * 保存玩家数据
     */
    public void savePlayerData() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }
        
        // 清空配置
        for (String key : dataConfig.getKeys(false)) {
            dataConfig.set(key, null);
        }
        
        // 保存所有玩家数据
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            
            String path = uuid.toString();
            dataConfig.set(path + ".lastTeleport", data.getLastTeleport());
            dataConfig.set(path + ".teleportCount", data.getTeleportCount());
            dataConfig.set(path + ".totalCost", data.getTotalCost());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存玩家数据: " + e.getMessage());
        }
    }
    
    /**
     * 获取玩家数据
     */
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }
    
    /**
     * 移除玩家数据
     */
    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }
    
    /**
     * 检查玩家是否在冷却中
     */
    public boolean isInCooldown(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        long timeSinceLastTeleport = System.currentTimeMillis() - data.getLastTeleport();
        int cooldown = getPlayerCooldown(uuid);
        return timeSinceLastTeleport < (cooldown * 1000L);
    }
    
    /**
     * 获取玩家剩余冷却时间（秒）
     */
    public long getRemainingCooldown(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        long timeSinceLastTeleport = System.currentTimeMillis() - data.getLastTeleport();
        int cooldown = getPlayerCooldown(uuid);
        long elapsed = timeSinceLastTeleport / 1000;
        return Math.max(0, cooldown - elapsed);
    }
    
    /**
     * 更新玩家传送时间
     */
    public void updateTeleportTime(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        data.setLastTeleport(System.currentTimeMillis());
        data.incrementTeleportCount();
    }
    
    /**
     * 获取玩家传送次数
     */
    public int getTeleportCount(UUID uuid) {
        return getPlayerData(uuid).getTeleportCount();
    }
    
    /**
     * 获取玩家总传送费用
     */
    public double getTotalCost(UUID uuid) {
        return getPlayerData(uuid).getTotalCost();
    }
    
    /**
     * 添加玩家传送费用
     */
    public void addCost(UUID uuid, double cost) {
        PlayerData data = getPlayerData(uuid);
        data.addTotalCost(cost);
    }
    
    /**
     * 获取玩家冷却时间（基于权限）
     */
    private int getPlayerCooldown(UUID uuid) {
        // 获取玩家权限
        org.bukkit.entity.Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            return plugin.getConfigManager().getCooldownTime();
        }
        
        if (player.hasPermission("rtp.vipplus")) {
            return plugin.getConfigManager().getPlayerCooldown("rtp.vipplus");
        } else if (player.hasPermission("rtp.vip")) {
            return plugin.getConfigManager().getPlayerCooldown("rtp.vip");
        } else {
            return plugin.getConfigManager().getPlayerCooldown("rtp.use");
        }
    }
    
    /**
     * 绕过玩家冷却
     */
    public void bypassCooldown(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        data.setLastTeleport(0);
    }
    
    /**
     * 清理过期的玩家数据（可选功能）
     */
    public void cleanupOldData() {
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime - (7 * 24 * 60 * 60 * 1000); // 7天
        
        playerDataMap.entrySet().removeIf(entry -> {
            PlayerData data = entry.getValue();
            return data.getLastTeleport() < expirationTime;
        });
    }
}