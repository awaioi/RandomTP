package org.awaioi.randomtp.logging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.awaioi.randomtp.RandomTP;

/**
 * 日志管理器
 * 负责管理插件的日志系统，包括日志轮转、归档和分级管理
 */
public class LogManager {
    
    private final RandomTP plugin;
    private final Logger logger;
    private final File logDirectory;
    private final File archiveDirectory;
    
    // 日志配置
    private LogLevelConfig logLevelConfig;
    private LogRotationConfig rotationConfig;
    private LogArchivalConfig archivalConfig;
    
    // 性能监控
    private final ConcurrentHashMap<String, Integer> logStats;
    private final ScheduledExecutorService scheduler;
    
    // 交易日志专用
    private static final String TRANSACTION_LOG_FILE = "transactions";
    private static final String PLAYER_DATA_LOG_FILE = "player_activity";
    
    public LogManager(RandomTP plugin) {
        this.plugin = plugin;
        this.logger = Logger.getLogger("RandomTP");
        this.logDirectory = new File(plugin.getDataFolder(), "logs");
        this.archiveDirectory = new File(plugin.getDataFolder(), "logs_archive");
        this.logStats = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        initializeLogDirectories();
        loadLogConfiguration();
        setupLogHandlers();
        startMaintenanceTasks();
    }
    
    /**
     * 初始化日志目录
     */
    private void initializeLogDirectories() {
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
        if (!archiveDirectory.exists()) {
            archiveDirectory.mkdirs();
        }
    }
    
    /**
     * 加载日志配置
     */
    private void loadLogConfiguration() {
        // 设置默认配置
        logLevelConfig = new LogLevelConfig();
        rotationConfig = new LogRotationConfig();
        archivalConfig = new LogArchivalConfig();
        
        // 从配置文件加载自定义设置
        if (plugin.getConfigManager().getConfig().contains("logging")) {
            org.bukkit.configuration.ConfigurationSection loggingConfig = 
                plugin.getConfigManager().getConfig().getConfigurationSection("logging");
            
            // 加载日志级别配置
            logLevelConfig.loadFromConfig(loggingConfig);
            
            // 加载轮转配置
            rotationConfig.loadFromConfig(loggingConfig);
            
            // 加载归档配置
            archivalConfig.loadFromConfig(loggingConfig);
        }
    }
    
    /**
     * 设置日志处理器
     */
    private void setupLogHandlers() {
        try {
            // 清除现有处理器
            logger.setUseParentHandlers(false);
            logger.getHandlers();
            
            // 主日志文件处理器
            FileHandler mainHandler = createFileHandler("main", rotationConfig.getMainLogRotation());
            mainHandler.setFormatter(new DetailedFormatter());
            logger.addHandler(mainHandler);
            
            // 交易日志处理器
            if (logLevelConfig.isTransactionLoggingEnabled()) {
                FileHandler transactionHandler = createFileHandler(TRANSACTION_LOG_FILE, 
                    rotationConfig.getTransactionLogRotation());
                transactionHandler.setFormatter(new TransactionFormatter());
                logger.addHandler(transactionHandler);
            }
            
            // 玩家活动日志处理器
            if (logLevelConfig.isPlayerActivityLoggingEnabled()) {
                FileHandler playerHandler = createFileHandler(PLAYER_DATA_LOG_FILE, 
                    rotationConfig.getPlayerLogRotation());
                playerHandler.setFormatter(new PlayerActivityFormatter());
                logger.addHandler(playerHandler);
            }
            
            // 控制台处理器（根据日志级别）
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(logLevelConfig.getConsoleLevel());
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);
            
        } catch (IOException e) {
            plugin.getLogger().severe("无法设置日志处理器: " + e.getMessage());
        }
    }
    
    /**
     * 创建文件处理器
     */
    private FileHandler createFileHandler(String logType, LogRotationConfig.RotationType rotationType) throws IOException {
        String fileName = getLogFileName(logType, rotationType);
        File logFile = new File(logDirectory, fileName);
        
        boolean append = rotationType == LogRotationConfig.RotationType.SIZE;
        FileHandler handler = new FileHandler(logFile.getAbsolutePath(), append);
        
        return handler;
    }
    
    /**
     * 获取日志文件名
     */
    private String getLogFileName(String logType, LogRotationConfig.RotationType rotationType) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = dateFormat.format(new Date());
        
        switch (rotationType) {
            case DAILY:
                return String.format("%s_%s.log", logType, dateStr);
            case HOURLY:
                SimpleDateFormat hourFormat = new SimpleDateFormat("yyyy-MM-dd_HH");
                return String.format("%s_%s.log", logType, hourFormat.format(new Date()));
            case SIZE:
                return String.format("%s.log", logType);
            default:
                return String.format("%s.log", logType);
        }
    }
    
    /**
     * 记录交易日志
     */
    public void logTransaction(String playerName, UUID playerUUID, String action, 
                             double amount, String description) {
        if (!logLevelConfig.isTransactionLoggingEnabled()) {
            return;
        }
        
        TransactionLogEntry entry = new TransactionLogEntry(
            System.currentTimeMillis(),
            playerName,
            playerUUID,
            action,
            amount,
            description
        );
        
        logger.log(Level.INFO, entry.toString());
        updateStats("transactions");
    }
    
    /**
     * 记录玩家活动日志
     */
    public void logPlayerActivity(String playerName, UUID playerUUID, String activity, 
                                String details) {
        if (!logLevelConfig.isPlayerActivityLoggingEnabled()) {
            return;
        }
        
        PlayerActivityLogEntry entry = new PlayerActivityLogEntry(
            System.currentTimeMillis(),
            playerName,
            playerUUID,
            activity,
            details
        );
        
        logger.log(Level.INFO, entry.toString());
        updateStats("player_activity");
    }
    
    /**
     * 记录系统日志
     */
    public void logSystem(String level, String message, Throwable throwable) {
        Level logLevel = Level.parse(level.toUpperCase());
        
        if (throwable != null) {
            logger.log(logLevel, message, throwable);
        } else {
            logger.log(logLevel, message);
        }
        
        updateStats("system");
    }
    
    /**
     * 记录经济系统日志
     */
    public void logEconomy(String playerName, UUID playerUUID, String operation, 
                          boolean success, String details) {
        if (!logLevelConfig.isEconomyLoggingEnabled()) {
            return;
        }
        
        EconomyLogEntry entry = new EconomyLogEntry(
            System.currentTimeMillis(),
            playerName,
            playerUUID,
            operation,
            success,
            details
        );
        
        logger.log(Level.INFO, entry.toString());
        updateStats("economy");
    }
    
    /**
     * 更新统计信息
     */
    private void updateStats(String logType) {
        logStats.merge(logType, 1, Integer::sum);
    }
    
    /**
     * 获取日志统计信息
     */
    public ConcurrentHashMap<String, Integer> getLogStats() {
        return new ConcurrentHashMap<>(logStats);
    }
    
    /**
     * 开始维护任务
     */
    private void startMaintenanceTasks() {
        // 每日日志轮转检查
        scheduler.scheduleAtFixedRate(this::checkLogRotation, 1, 1, TimeUnit.HOURS);
        
        // 每周归档清理
        scheduler.scheduleAtFixedRate(this::archiveOldLogs, 1, 7, TimeUnit.DAYS);
        
        // 每月完全清理
        scheduler.scheduleAtFixedRate(this::monthlyCleanup, 1, 30, TimeUnit.DAYS);
    }
    
    /**
     * 检查日志轮转
     */
    private void checkLogRotation() {
        // 检查文件大小，触发轮转
        for (File logFile : logDirectory.listFiles((dir, name) -> name.endsWith(".log"))) {
            long fileSize = logFile.length();
            long maxSize = rotationConfig.getMaxFileSize();
            
            if (fileSize > maxSize) {
                rotateLogFile(logFile);
            }
        }
    }
    
    /**
     * 轮转日志文件
     */
    private void rotateLogFile(File logFile) {
        try {
            String baseName = logFile.getName().replace(".log", "");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            
            File archivedFile = new File(logDirectory, baseName + "_" + timestamp + ".log");
            boolean success = logFile.renameTo(archivedFile);
            
            if (success) {
                plugin.getLogger().info("日志文件已轮转: " + archivedFile.getName());
            } else {
                plugin.getLogger().warning("日志文件轮转失败: " + logFile.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("日志轮转异常: " + e.getMessage());
        }
    }
    
    /**
     * 归档旧日志
     */
    private void archiveOldLogs() {
        long cutoffTime = System.currentTimeMillis() - (archivalConfig.getArchiveAfterDays() * 24 * 60 * 60 * 1000L);
        
        for (File logFile : logDirectory.listFiles((dir, name) -> name.endsWith(".log"))) {
            if (logFile.lastModified() < cutoffTime) {
                archiveLogFile(logFile);
            }
        }
    }
    
    /**
     * 归档日志文件
     */
    private void archiveLogFile(File logFile) {
        try {
            String fileName = logFile.getName();
            File archiveFile = new File(archiveDirectory, fileName);
            
            boolean success = logFile.renameTo(archiveFile);
            
            if (success) {
                plugin.getLogger().info("日志文件已归档: " + archiveFile.getName());
                compressArchivedFile(archiveFile);
            } else {
                plugin.getLogger().warning("日志文件归档失败: " + logFile.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("日志归档异常: " + e.getMessage());
        }
    }
    
    /**
     * 压缩归档文件
     */
    private void compressArchivedFile(File file) {
        // 这里可以实现GZIP压缩
        // 为简化示例，暂时跳过压缩实现
    }
    
    /**
     * 月度清理
     */
    private void monthlyCleanup() {
        long cutoffTime = System.currentTimeMillis() - (archivalConfig.getDeleteAfterDays() * 24 * 60 * 60 * 1000L);
        
        for (File file : archiveDirectory.listFiles()) {
            if (file.lastModified() < cutoffTime) {
                boolean deleted = file.delete();
                if (deleted) {
                    plugin.getLogger().info("已删除过期日志文件: " + file.getName());
                }
            }
        }
    }
    
    /**
     * 获取日志目录信息
     */
    public LogDirectoryInfo getLogDirectoryInfo() {
        long totalSize = 0;
        int fileCount = 0;
        
        for (File file : logDirectory.listFiles()) {
            if (file.isFile()) {
                totalSize += file.length();
                fileCount++;
            }
        }
        
        for (File file : archiveDirectory.listFiles()) {
            if (file.isFile()) {
                totalSize += file.length();
                fileCount++;
            }
        }
        
        return new LogDirectoryInfo(totalSize, fileCount);
    }
    
    /**
     * 清理内存中的统计数据
     */
    public void clearStats() {
        logStats.clear();
    }
    
    /**
     * 关闭日志管理器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        for (Handler handler : logger.getHandlers()) {
            handler.close();
        }
    }
    
    // 内部类定义
    
    /**
     * 日志级别配置
     */
    public static class LogLevelConfig {
        private boolean transactionLoggingEnabled = true;
        private boolean playerActivityLoggingEnabled = true;
        private boolean economyLoggingEnabled = true;
        private Level consoleLevel = Level.INFO;
        private Level fileLevel = Level.ALL;
        
        public void loadFromConfig(org.bukkit.configuration.ConfigurationSection config) {
            if (config.contains("enabled.transaction-logging")) {
                transactionLoggingEnabled = config.getBoolean("enabled.transaction-logging", true);
            }
            if (config.contains("enabled.player-activity")) {
                playerActivityLoggingEnabled = config.getBoolean("enabled.player-activity", true);
            }
            if (config.contains("enabled.economy")) {
                economyLoggingEnabled = config.getBoolean("enabled.economy", true);
            }
            if (config.contains("levels.console")) {
                consoleLevel = Level.parse(config.getString("levels.console", "INFO"));
            }
            if (config.contains("levels.file")) {
                fileLevel = Level.parse(config.getString("levels.file", "ALL"));
            }
        }
        
        // Getters
        public boolean isTransactionLoggingEnabled() { return transactionLoggingEnabled; }
        public boolean isPlayerActivityLoggingEnabled() { return playerActivityLoggingEnabled; }
        public boolean isEconomyLoggingEnabled() { return economyLoggingEnabled; }
        public Level getConsoleLevel() { return consoleLevel; }
        public Level getFileLevel() { return fileLevel; }
    }
    
    /**
     * 日志轮转配置
     */
    public static class LogRotationConfig {
        public enum RotationType { DAILY, HOURLY, SIZE, NEVER }
        
        private RotationType mainLogRotation = RotationType.DAILY;
        private RotationType transactionLogRotation = RotationType.DAILY;
        private RotationType playerLogRotation = RotationType.DAILY;
        private long maxFileSize = 10 * 1024 * 1024; // 10MB
        
        public void loadFromConfig(org.bukkit.configuration.ConfigurationSection config) {
            if (config.contains("rotation.main")) {
                mainLogRotation = RotationType.valueOf(config.getString("rotation.main", "DAILY").toUpperCase());
            }
            if (config.contains("rotation.transaction")) {
                transactionLogRotation = RotationType.valueOf(config.getString("rotation.transaction", "DAILY").toUpperCase());
            }
            if (config.contains("rotation.player-activity")) {
                playerLogRotation = RotationType.valueOf(config.getString("rotation.player-activity", "DAILY").toUpperCase());
            }
            if (config.contains("rotation.max-file-size")) {
                maxFileSize = config.getLong("rotation.max-file-size", 10485760);
            }
        }
        
        // Getters
        public RotationType getMainLogRotation() { return mainLogRotation; }
        public RotationType getTransactionLogRotation() { return transactionLogRotation; }
        public RotationType getPlayerLogRotation() { return playerLogRotation; }
        public long getMaxFileSize() { return maxFileSize; }
    }
    
    /**
     * 日志归档配置
     */
    public static class LogArchivalConfig {
        private int archiveAfterDays = 7;
        private int deleteAfterDays = 30;
        private boolean compressArchived = true;
        
        public void loadFromConfig(org.bukkit.configuration.ConfigurationSection config) {
            if (config.contains("archival.archive-after-days")) {
                archiveAfterDays = config.getInt("archival.archive-after-days", 7);
            }
            if (config.contains("archival.delete-after-days")) {
                deleteAfterDays = config.getInt("archival.delete-after-days", 30);
            }
            if (config.contains("archival.compress")) {
                compressArchived = config.getBoolean("archival.compress", true);
            }
        }
        
        // Getters
        public int getArchiveAfterDays() { return archiveAfterDays; }
        public int getDeleteAfterDays() { return deleteAfterDays; }
        public boolean isCompressArchived() { return compressArchived; }
    }
    
    /**
     * 日志目录信息
     */
    public static class LogDirectoryInfo {
        private final long totalSize;
        private final int fileCount;
        
        public LogDirectoryInfo(long totalSize, int fileCount) {
            this.totalSize = totalSize;
            this.fileCount = fileCount;
        }
        
        public long getTotalSize() { return totalSize; }
        public int getFileCount() { return fileCount; }
        
        public String getFormattedSize() {
            if (totalSize < 1024) return totalSize + " B";
            if (totalSize < 1024 * 1024) return String.format("%.2f KB", totalSize / 1024.0);
            return String.format("%.2f MB", totalSize / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 交易日志条目
     */
    public static class TransactionLogEntry {
        private final long timestamp;
        private final String playerName;
        private final UUID playerUUID;
        private final String action;
        private final double amount;
        private final String description;
        
        public TransactionLogEntry(long timestamp, String playerName, UUID playerUUID, 
                                 String action, double amount, String description) {
            this.timestamp = timestamp;
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.action = action;
            this.amount = amount;
            this.description = description;
        }
        
        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return String.format("[TRANSACTION] %s | Player: %s (%s) | Action: %s | Amount: %.2f | %s",
                dateFormat.format(new Date(timestamp)), playerName, playerUUID, action, amount, description);
        }
    }
    
    /**
     * 玩家活动日志条目
     */
    public static class PlayerActivityLogEntry {
        private final long timestamp;
        private final String playerName;
        private final UUID playerUUID;
        private final String activity;
        private final String details;
        
        public PlayerActivityLogEntry(long timestamp, String playerName, UUID playerUUID, 
                                    String activity, String details) {
            this.timestamp = timestamp;
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.activity = activity;
            this.details = details;
        }
        
        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return String.format("[PLAYER_ACTIVITY] %s | Player: %s (%s) | Activity: %s | Details: %s",
                dateFormat.format(new Date(timestamp)), playerName, playerUUID, activity, details);
        }
    }
    
    /**
     * 经济日志条目
     */
    public static class EconomyLogEntry {
        private final long timestamp;
        private final String playerName;
        private final UUID playerUUID;
        private final String operation;
        private final boolean success;
        private final String details;
        
        public EconomyLogEntry(long timestamp, String playerName, UUID playerUUID, 
                             String operation, boolean success, String details) {
            this.timestamp = timestamp;
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.operation = operation;
            this.success = success;
            this.details = details;
        }
        
        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String status = success ? "SUCCESS" : "FAILED";
            return String.format("[ECONOMY] %s | Player: %s (%s) | Operation: %s | Status: %s | Details: %s",
                dateFormat.format(new Date(timestamp)), playerName, playerUUID, operation, status, details);
        }
    }
    
    /**
     * 详细日志格式化器
     */
    public static class DetailedFormatter extends SimpleFormatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        @Override
        public String format(LogRecord record) {
            return String.format("[%s] [%s] %s%n", 
                dateFormat.format(new Date(record.getMillis())),
                record.getLevel().getName(),
                record.getMessage());
        }
    }
    
    /**
     * 交易日志格式化器
     */
    public static class TransactionFormatter extends SimpleFormatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        @Override
        public String format(LogRecord record) {
            return String.format("%s | %s%n", 
                dateFormat.format(new Date(record.getMillis())),
                record.getMessage());
        }
    }
    
    /**
     * 玩家活动日志格式化器
     */
    public static class PlayerActivityFormatter extends SimpleFormatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        @Override
        public String format(LogRecord record) {
            return String.format("%s | %s%n", 
                dateFormat.format(new Date(record.getMillis())),
                record.getMessage());
        }
    }
}