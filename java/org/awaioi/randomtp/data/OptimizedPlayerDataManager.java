package org.awaioi.randomtp.data;

import org.awaioi.randomtp.RandomTP;
import org.awaioi.randomtp.logging.LogManager;
import org.awaioi.randomtp.logging.LogManager.PlayerActivityLogEntry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 优化的玩家数据管理器
 * 解决了playerdata.yml文件大小问题，提供了数据清理、归档和优化功能
 */
public class OptimizedPlayerDataManager {
    
    private final RandomTP plugin;
    private final LogManager logManager;
    private final Map<UUID, PlayerData> playerDataMap;
    private final Map<UUID, PlayerDataStats> playerStatsMap;
    
    // 文件管理
    private File activeDataFile;
    private File archiveDataFile;
    private FileConfiguration activeConfig;
    private FileConfiguration archiveConfig;
    
    // 优化配置
    private final DataOptimizationConfig optimizationConfig;
    
    // 缓存和性能优化
    private final ConcurrentHashMap<UUID, Long> lastAccessTime;
    private final ScheduledExecutorService maintenanceScheduler;
    
    // 统计信息
    private final PlayerDataStatistics statistics;
    
    public OptimizedPlayerDataManager(RandomTP plugin, LogManager logManager) {
        this.plugin = plugin;
        this.logManager = logManager;
        this.playerDataMap = new ConcurrentHashMap<>();
        this.playerStatsMap = new ConcurrentHashMap<>();
        this.lastAccessTime = new ConcurrentHashMap<>();
        this.maintenanceScheduler = Executors.newScheduledThreadPool(2);
        this.optimizationConfig = new DataOptimizationConfig();
        this.statistics = new PlayerDataStatistics();
        
        initializeDataFiles();
        loadOptimizationConfig();
        loadActivePlayerData();
        startMaintenanceTasks();
    }
    
    /**
     * 初始化数据文件
     */
    private void initializeDataFiles() {
        File dataDirectory = plugin.getDataFolder();
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        
        activeDataFile = new File(dataDirectory, "playerdata_active.yml");
        archiveDataFile = new File(dataDirectory, "playerdata_archive.yml");
        
        // 创建文件（如果不存在）
        if (!activeDataFile.exists()) {
            try {
                activeDataFile.createNewFile();
                plugin.getLogger().info("已创建活动玩家数据文件");
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建活动玩家数据文件: " + e.getMessage());
            }
        }
        
        if (!archiveDataFile.exists()) {
            try {
                archiveDataFile.createNewFile();
                plugin.getLogger().info("已创建归档玩家数据文件");
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建归档玩家数据文件: " + e.getMessage());
            }
        }
    }
    
    /**
     * 加载优化配置
     */
    private void loadOptimizationConfig() {
        // 从配置文件加载优化设置
        if (plugin.getConfigManager().getConfig().contains("data-optimization")) {
            org.bukkit.configuration.ConfigurationSection config = 
                plugin.getConfigManager().getConfig().getConfigurationSection("data-optimization");
            
            optimizationConfig.loadFromConfig(config);
        }
    }
    
    /**
     * 加载活动玩家数据
     */
    private void loadActivePlayerData() {
        try {
            activeConfig = YamlConfiguration.loadConfiguration(activeDataFile);
            
            int loadedCount = 0;
            long currentTime = System.currentTimeMillis();
            
            // 加载活跃玩家数据
            for (String uuidStr : activeConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    PlayerData playerData = new PlayerData(uuid);
                    
                    playerData.setLastTeleport(activeConfig.getLong(uuidStr + ".lastTeleport"));
                    playerData.setTeleportCount(activeConfig.getInt(uuidStr + ".teleportCount"));
                    playerData.setTotalCost(activeConfig.getDouble(uuidStr + ".totalCost", 0.0));
                    
                    playerDataMap.put(uuid, playerData);
                    playerStatsMap.put(uuid, loadPlayerStats(uuidStr));
                    lastAccessTime.put(uuid, currentTime);
                    
                    loadedCount++;
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的UUID格式: " + uuidStr);
                }
            }
            
            plugin.getLogger().info("已加载 " + loadedCount + " 个活跃玩家的数据");
            statistics.setLoadedPlayers(loadedCount);
            
            // 记录加载日志
            logManager.logSystem("INFO", 
                "PlayerDataManager: 加载了 " + loadedCount + " 个玩家数据", null);
            
        } catch (Exception e) {
            plugin.getLogger().severe("加载玩家数据失败: " + e.getMessage());
            logManager.logSystem("SEVERE", "PlayerDataManager: 加载数据失败", e);
        }
    }
    
    /**
     * 加载玩家统计信息
     */
    private PlayerDataStats loadPlayerStats(String uuidStr) {
        PlayerDataStats stats = new PlayerDataStats();
        
        if (activeConfig.contains(uuidStr + ".stats")) {
            stats.setFirstJoin(activeConfig.getLong(uuidStr + ".stats.firstJoin"));
            stats.setLastActive(activeConfig.getLong(uuidStr + ".stats.lastActive"));
            stats.setTotalOnlineTime(activeConfig.getLong(uuidStr + ".stats.totalOnlineTime"));
            stats.setAverageTeleportInterval(activeConfig.getLong(uuidStr + ".stats.averageInterval"));
            stats.setFavoriteServers(activeConfig.getStringList(uuidStr + ".stats.favoriteServers"));
        }
        
        return stats;
    }
    
    /**
     * 获取玩家数据（带访问时间更新）
     */
    public PlayerData getPlayerData(UUID uuid) {
        PlayerData data = playerDataMap.computeIfAbsent(uuid, PlayerData::new);
        lastAccessTime.put(uuid, System.currentTimeMillis());
        
        // 更新玩家统计
        updatePlayerStatistics(uuid, data);
        
        return data;
    }
    
    /**
     * 更新玩家统计信息
     */
    private void updatePlayerStatistics(UUID uuid, PlayerData data) {
        PlayerDataStats stats = playerStatsMap.computeIfAbsent(uuid, k -> new PlayerDataStats());
        
        stats.setLastActive(System.currentTimeMillis());
        
        // 如果是第一次记录，初始化首次加入时间
        if (stats.getFirstJoin() == 0) {
            stats.setFirstJoin(System.currentTimeMillis());
        }
        
        // 更新平均传送间隔
        long currentTime = System.currentTimeMillis();
        long lastTeleport = data.getLastTeleport();
        
        if (lastTeleport > 0) {
            long interval = currentTime - lastTeleport;
            long currentAvg = stats.getAverageTeleportInterval();
            
            if (currentAvg == 0) {
                stats.setAverageTeleportInterval(interval);
            } else {
                // 计算新的平均值
                long newAvg = (currentAvg + interval) / 2;
                stats.setAverageTeleportInterval(newAvg);
            }
        }
        
        // 更新在线时间（基于冷却时间推算）
        updateOnlineTime(uuid);
    }
    
    /**
     * 更新在线时间
     */
    private void updateOnlineTime(UUID uuid) {
        // 这里可以根据实际情况实现在线时间统计
        // 简化示例：根据冷却时间推断
    }
    
    /**
     * 保存活动玩家数据
     */
    public void saveActivePlayerData() {
        try {
            if (activeConfig == null) {
                activeConfig = new YamlConfiguration();
            }
            
            // 只保存活跃玩家（最近访问过的）
            long currentTime = System.currentTimeMillis();
            long inactiveThreshold = optimizationConfig.getInactiveThresholdDays() * 24 * 60 * 60 * 1000L;
            
            // 分离活跃和非活跃玩家
            Map<UUID, PlayerData> activePlayers = new HashMap<>();
            Map<UUID, PlayerData> inactivePlayers = new HashMap<>();
            
            for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
                UUID uuid = entry.getKey();
                Long lastAccess = lastAccessTime.get(uuid);
                
                if (lastAccess != null && (currentTime - lastAccess) < inactiveThreshold) {
                    activePlayers.put(uuid, entry.getValue());
                } else {
                    inactivePlayers.put(uuid, entry.getValue());
                }
            }
            
            // 清空配置
            for (String key : activeConfig.getKeys(false)) {
                activeConfig.set(key, null);
            }
            
            // 保存活跃玩家数据
            for (Map.Entry<UUID, PlayerData> entry : activePlayers.entrySet()) {
                savePlayerDataToConfig(entry.getKey(), entry.getValue(), activeConfig);
            }
            
            // 保存到文件
            activeConfig.save(activeDataFile);
            
            // 归档非活跃玩家数据
            if (!inactivePlayers.isEmpty()) {
                archiveInactivePlayers(inactivePlayers);
            }
            
            // 更新统计信息
            statistics.setActivePlayers(activePlayers.size());
            statistics.setInactivePlayers(inactivePlayers.size());
            statistics.setLastSaveTime(currentTime);
            
            plugin.getLogger().info("已保存 " + activePlayers.size() + " 个活跃玩家数据，" +
                                  "归档 " + inactivePlayers.size() + " 个非活跃玩家数据");
            
            // 记录保存日志
            logManager.logSystem("INFO", 
                String.format("PlayerDataManager: 保存了 %d 个活跃玩家，归档了 %d 个非活跃玩家",
                    activePlayers.size(), inactivePlayers.size()), null);
            
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + e.getMessage());
            logManager.logSystem("SEVERE", "PlayerDataManager: 保存数据失败", e);
        }
    }
    
    /**
     * 保存玩家数据到配置
     */
    private void savePlayerDataToConfig(UUID uuid, PlayerData data, FileConfiguration config) {
        String path = uuid.toString();
        
        config.set(path + ".lastTeleport", data.getLastTeleport());
        config.set(path + ".teleportCount", data.getTeleportCount());
        config.set(path + ".totalCost", data.getTotalCost());
        
        // 保存统计信息
        PlayerDataStats stats = playerStatsMap.get(uuid);
        if (stats != null) {
            config.set(path + ".stats.firstJoin", stats.getFirstJoin());
            config.set(path + ".stats.lastActive", stats.getLastActive());
            config.set(path + ".stats.totalOnlineTime", stats.getTotalOnlineTime());
            config.set(path + ".stats.averageInterval", stats.getAverageTeleportInterval());
            config.set(path + ".stats.favoriteServers", stats.getFavoriteServers());
        }
    }
    
    /**
     * 归档非活跃玩家
     */
    private void archiveInactivePlayers(Map<UUID, PlayerData> inactivePlayers) {
        try {
            archiveConfig = YamlConfiguration.loadConfiguration(archiveDataFile);
            
            for (Map.Entry<UUID, PlayerData> entry : inactivePlayers.entrySet()) {
                savePlayerDataToConfig(entry.getKey(), entry.getValue(), archiveConfig);
                
                // 从内存中移除
                playerDataMap.remove(entry.getKey());
                playerStatsMap.remove(entry.getKey());
                lastAccessTime.remove(entry.getKey());
            }
            
            archiveConfig.save(archiveDataFile);
            
            plugin.getLogger().info("已归档 " + inactivePlayers.size() + " 个非活跃玩家数据");
            
        } catch (IOException e) {
            plugin.getLogger().severe("归档玩家数据失败: " + e.getMessage());
            logManager.logSystem("SEVERE", "PlayerDataManager: 归档数据失败", e);
        }
    }
    
    /**
     * 开始维护任务
     */
    private void startMaintenanceTasks() {
        // 定期保存数据
        maintenanceScheduler.scheduleAtFixedRate(this::saveActivePlayerData, 
            optimizationConfig.getAutoSaveIntervalMinutes(), 
            optimizationConfig.getAutoSaveIntervalMinutes(), 
            TimeUnit.MINUTES);
        
        // 定期清理过期数据
        maintenanceScheduler.scheduleAtFixedRate(this::cleanupExpiredData,
            1, 1, TimeUnit.HOURS);
        
        // 定期优化文件
        maintenanceScheduler.scheduleAtFixedRate(this::optimizeDataFiles,
            1, 6, TimeUnit.HOURS);
        
        // 每日深度清理
        maintenanceScheduler.scheduleAtFixedRate(this::dailyDeepCleanup,
            1, 24, TimeUnit.HOURS);
    }
    
    /**
     * 清理过期数据
     */
    private void cleanupExpiredData() {
        long currentTime = System.currentTimeMillis();
        long expirationTime = optimizationConfig.getDataExpirationDays() * 24 * 60 * 60 * 1000L;
        long cutoffTime = currentTime - expirationTime;
        
        int cleanedCount = 0;
        
        // 清理过期的归档数据
        try {
            if (archiveConfig == null) {
                archiveConfig = YamlConfiguration.loadConfiguration(archiveDataFile);
            }
            
            List<String> keysToRemove = new ArrayList<>();
            
            for (String uuidStr : archiveConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    PlayerDataStats stats = loadPlayerStats(uuidStr);
                    
                    if (stats.getLastActive() < cutoffTime) {
                        keysToRemove.add(uuidStr);
                        cleanedCount++;
                    }
                } catch (IllegalArgumentException e) {
                    keysToRemove.add(uuidStr); // 无效UUID也清理
                    cleanedCount++;
                }
            }
            
            // 移除过期数据
            for (String key : keysToRemove) {
                archiveConfig.set(key, null);
            }
            
            if (!keysToRemove.isEmpty()) {
                archiveConfig.save(archiveDataFile);
                plugin.getLogger().info("已清理 " + cleanedCount + " 个过期玩家数据");
                
                logManager.logSystem("INFO", 
                    "PlayerDataManager: 清理了 " + cleanedCount + " 个过期数据", null);
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("清理过期数据失败: " + e.getMessage());
            logManager.logSystem("SEVERE", "PlayerDataManager: 清理过期数据失败", e);
        }
    }
    
    /**
     * 优化数据文件
     */
    private void optimizeDataFiles() {
        try {
            // 检查文件大小
            long activeSize = activeDataFile.length();
            long archiveSize = archiveDataFile.length();
            
            if (activeSize > optimizationConfig.getMaxActiveFileSize()) {
                plugin.getLogger().warning("活动数据文件过大 (" + activeSize + " bytes)，触发优化");
                optimizeActiveFile();
            }
            
            if (archiveSize > optimizationConfig.getMaxArchiveFileSize()) {
                plugin.getLogger().warning("归档数据文件过大 (" + archiveSize + " bytes)，触发优化");
                optimizeArchiveFile();
            }
            
            // 更新统计信息
            statistics.setActiveFileSize(activeSize);
            statistics.setArchiveFileSize(archiveSize);
            
        } catch (Exception e) {
            plugin.getLogger().severe("优化数据文件失败: " + e.getMessage());
            logManager.logSystem("SEVERE", "PlayerDataManager: 优化文件失败", e);
        }
    }
    
    /**
     * 优化活动文件
     */
    private void optimizeActiveFile() {
        // 重新加载并重新保存，以去除碎片
        try {
            activeConfig = YamlConfiguration.loadConfiguration(activeDataFile);
            activeConfig.save(activeDataFile);
            
            plugin.getLogger().info("活动数据文件优化完成");
            logManager.logSystem("INFO", "PlayerDataManager: 活动文件优化完成", null);
            
        } catch (IOException e) {
            plugin.getLogger().severe("优化活动文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 优化归档文件
     */
    private void optimizeArchiveFile() {
        // 可以实现更复杂的归档优化，如压缩、分割等
        try {
            archiveConfig = YamlConfiguration.loadConfiguration(archiveDataFile);
            
            // 清理无效数据
            List<String> invalidKeys = new ArrayList<>();
            for (String key : archiveConfig.getKeys(false)) {
                if (!isValidPlayerData(archiveConfig, key)) {
                    invalidKeys.add(key);
                }
            }
            
            for (String key : invalidKeys) {
                archiveConfig.set(key, null);
            }
            
            archiveConfig.save(archiveDataFile);
            
            plugin.getLogger().info("归档数据文件优化完成，清理了 " + invalidKeys.size() + " 个无效条目");
            
        } catch (IOException e) {
            plugin.getLogger().severe("优化归档文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证玩家数据有效性
     */
    private boolean isValidPlayerData(FileConfiguration config, String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            
            // 检查必要字段是否存在
            return config.contains(uuidStr + ".lastTeleport") &&
                   config.contains(uuidStr + ".teleportCount") &&
                   config.contains(uuidStr + ".totalCost");
                   
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 每日深度清理
     */
    private void dailyDeepCleanup() {
        plugin.getLogger().info("开始每日深度数据清理...");
        
        try {
            // 清理统计信息中的过期条目
            cleanupExpiredStatistics();
            
            // 重建索引
            rebuildPlayerIndex();
            
            // 生成清理报告
            generateCleanupReport();
            
        } catch (Exception e) {
            plugin.getLogger().severe("每日深度清理失败: " + e.getMessage());
            logManager.logSystem("SEVERE", "PlayerDataManager: 每日深度清理失败", e);
        }
    }
    
    /**
     * 清理过期统计信息
     */
    private void cleanupExpiredStatistics() {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - (optimizationConfig.getDataExpirationDays() * 24 * 60 * 60 * 1000L);
        
        playerStatsMap.entrySet().removeIf(entry -> {
            PlayerDataStats stats = entry.getValue();
            return stats.getLastActive() < cutoffTime;
        });
    }
    
    /**
     * 重建玩家索引
     */
    private void rebuildPlayerIndex() {
        // 重建lastAccessTime索引
        lastAccessTime.clear();
        for (UUID uuid : playerDataMap.keySet()) {
            lastAccessTime.put(uuid, System.currentTimeMillis());
        }
    }
    
    /**
     * 生成清理报告
     */
    private void generateCleanupReport() {
        int totalPlayers = playerDataMap.size() + playerStatsMap.size();
        long totalSize = activeDataFile.length() + archiveDataFile.length();
        
        String report = String.format(
            "数据清理报告 - 玩家总数: %d, 活跃玩家: %d, 总文件大小: %.2f MB",
            totalPlayers, playerDataMap.size(), totalSize / (1024.0 * 1024.0)
        );
        
        plugin.getLogger().info(report);
        logManager.logSystem("INFO", "PlayerDataManager: " + report, null);
    }
    
    /**
     * 记录玩家活动
     */
    public void logPlayerActivity(UUID uuid, String playerName, String activity, String details) {
        PlayerData data = getPlayerData(uuid);
        
        // 记录到日志管理器
        logManager.logPlayerActivity(playerName, uuid, activity, details);
        
        // 记录到内部统计
        statistics.incrementActivityCount(activity);
    }
    
    /**
     * 获取玩家数据统计
     */
    public PlayerDataStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * 获取优化配置
     */
    public DataOptimizationConfig getOptimizationConfig() {
        return optimizationConfig;
    }
    
    /**
     * 手动触发保存
     */
    public void forceSave() {
        saveActivePlayerData();
    }
    
    /**
     * 手动触发清理
     */
    public void forceCleanup() {
        cleanupExpiredData();
        optimizeDataFiles();
        dailyDeepCleanup();
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        // 保存所有数据
        saveActivePlayerData();
        
        // 关闭调度器
        maintenanceScheduler.shutdown();
        try {
            if (!maintenanceScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            maintenanceScheduler.shutdownNow();
        }
    }
    
    // 内部类定义
    
    /**
     * 玩家数据统计信息
     */
    public static class PlayerDataStatistics {
        private int loadedPlayers = 0;
        private int activePlayers = 0;
        private int inactivePlayers = 0;
        private long lastSaveTime = 0;
        private long activeFileSize = 0;
        private long archiveFileSize = 0;
        private final ConcurrentHashMap<String, Integer> activityCounts = new ConcurrentHashMap<>();
        
        // Getters and Setters
        public int getLoadedPlayers() { return loadedPlayers; }
        public void setLoadedPlayers(int loadedPlayers) { this.loadedPlayers = loadedPlayers; }
        
        public int getActivePlayers() { return activePlayers; }
        public void setActivePlayers(int activePlayers) { this.activePlayers = activePlayers; }
        
        public int getInactivePlayers() { return inactivePlayers; }
        public void setInactivePlayers(int inactivePlayers) { this.inactivePlayers = inactivePlayers; }
        
        public long getLastSaveTime() { return lastSaveTime; }
        public void setLastSaveTime(long lastSaveTime) { this.lastSaveTime = lastSaveTime; }
        
        public long getActiveFileSize() { return activeFileSize; }
        public void setActiveFileSize(long activeFileSize) { this.activeFileSize = activeFileSize; }
        
        public long getArchiveFileSize() { return archiveFileSize; }
        public void setArchiveFileSize(long archiveFileSize) { this.archiveFileSize = archiveFileSize; }
        
        public ConcurrentHashMap<String, Integer> getActivityCounts() { return activityCounts; }
        
        public void incrementActivityCount(String activity) {
            activityCounts.merge(activity, 1, Integer::sum);
        }
        
        public long getTotalFileSize() {
            return activeFileSize + archiveFileSize;
        }
        
        public String getFormattedTotalSize() {
            long totalSize = getTotalFileSize();
            if (totalSize < 1024) return totalSize + " B";
            if (totalSize < 1024 * 1024) return String.format("%.2f KB", totalSize / 1024.0);
            return String.format("%.2f MB", totalSize / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 玩家数据统计详情
     */
    public static class PlayerDataStats {
        private long firstJoin = 0;
        private long lastActive = 0;
        private long totalOnlineTime = 0;
        private long averageTeleportInterval = 0;
        private List<String> favoriteServers = new ArrayList<>();
        
        // Getters and Setters
        public long getFirstJoin() { return firstJoin; }
        public void setFirstJoin(long firstJoin) { this.firstJoin = firstJoin; }
        
        public long getLastActive() { return lastActive; }
        public void setLastActive(long lastActive) { this.lastActive = lastActive; }
        
        public long getTotalOnlineTime() { return totalOnlineTime; }
        public void setTotalOnlineTime(long totalOnlineTime) { this.totalOnlineTime = totalOnlineTime; }
        
        public long getAverageTeleportInterval() { return averageTeleportInterval; }
        public void setAverageTeleportInterval(long averageTeleportInterval) { this.averageTeleportInterval = averageTeleportInterval; }
        
        public List<String> getFavoriteServers() { return favoriteServers; }
        public void setFavoriteServers(List<String> favoriteServers) { this.favoriteServers = favoriteServers; }
    }
    
    /**
     * 数据优化配置
     */
    public static class DataOptimizationConfig {
        private int inactiveThresholdDays = 30;
        private int dataExpirationDays = 90;
        private int autoSaveIntervalMinutes = 15;
        private long maxActiveFileSize = 5 * 1024 * 1024; // 5MB
        private long maxArchiveFileSize = 50 * 1024 * 1024; // 50MB
        private boolean enableCompression = true;
        private boolean enableStatistics = true;
        
        public void loadFromConfig(org.bukkit.configuration.ConfigurationSection config) {
            if (config.contains("inactive-threshold-days")) {
                inactiveThresholdDays = config.getInt("inactive-threshold-days", 30);
            }
            if (config.contains("data-expiration-days")) {
                dataExpirationDays = config.getInt("data-expiration-days", 90);
            }
            if (config.contains("auto-save-interval-minutes")) {
                autoSaveIntervalMinutes = config.getInt("auto-save-interval-minutes", 15);
            }
            if (config.contains("max-active-file-size")) {
                maxActiveFileSize = config.getLong("max-active-file-size", 5242880);
            }
            if (config.contains("max-archive-file-size")) {
                maxArchiveFileSize = config.getLong("max-archive-file-size", 52428800);
            }
            if (config.contains("enable-compression")) {
                enableCompression = config.getBoolean("enable-compression", true);
            }
            if (config.contains("enable-statistics")) {
                enableStatistics = config.getBoolean("enable-statistics", true);
            }
        }
        
        // Getters
        public int getInactiveThresholdDays() { return inactiveThresholdDays; }
        public int getDataExpirationDays() { return dataExpirationDays; }
        public int getAutoSaveIntervalMinutes() { return autoSaveIntervalMinutes; }
        public long getMaxActiveFileSize() { return maxActiveFileSize; }
        public long getMaxArchiveFileSize() { return maxArchiveFileSize; }
        public boolean isEnableCompression() { return enableCompression; }
        public boolean isEnableStatistics() { return enableStatistics; }
    }
}