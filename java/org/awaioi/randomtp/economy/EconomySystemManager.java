package org.awaioi.randomtp.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.awaioi.randomtp.RandomTP;
import org.awaioi.randomtp.config.ConfigManager;
import org.awaioi.randomtp.economy.adapter.EconomyAdapter;
import org.awaioi.randomtp.economy.adapter.EssentialsXEconomyAdapter;
import org.awaioi.randomtp.economy.adapter.VaultEconomyAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 经济系统管理器
 * 自动识别和适配多种经济插件
 */
public class EconomySystemManager {
    
    private final RandomTP plugin;
    private EconomyAdapter activeAdapter;
    private final Map<String, EconomyAdapter> availableAdapters;
    private final Map<UUID, Double> playerMoneyCache;
    private boolean enabled;
    private boolean initialized;
    private BukkitRunnable detectionTask;
    
    public EconomySystemManager(RandomTP plugin) {
        this.plugin = plugin;
        this.availableAdapters = new ConcurrentHashMap<>();
        this.playerMoneyCache = new ConcurrentHashMap<>();
        this.enabled = false;
        this.initialized = false;
        this.activeAdapter = null;
    }
    
    /**
     * 初始化经济系统管理器
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        plugin.getLogger().info("正在初始化经济系统管理器...");
        
        // 检查配置
        if (!plugin.getConfigManager().isEconomyEnabled()) {
            plugin.getLogger().info("经济系统已禁用");
            enabled = false;
            initialized = true;
            return;
        }
        
        // 注册适配器
        registerAdapters();
        
        // 检测经济插件
        detectEconomyPlugins();
        
        // 选择最佳适配器
        selectBestAdapter();
        
        // 启动定期检测任务
        startDetectionTask();
        
        initialized = true;
        
        if (activeAdapter != null) {
            enabled = true;
            plugin.getLogger().info("经济系统初始化成功 - 使用: " + activeAdapter.getName());
        } else {
            enabled = false;
            plugin.getLogger().warning("未检测到可用的经济插件，经济功能将被禁用");
        }
    }
    
    /**
     * 注册所有适配器
     */
    private void registerAdapters() {
        // 注册Vault适配器
        VaultEconomyAdapter vaultAdapter = new VaultEconomyAdapter();
        availableAdapters.put("vault", vaultAdapter);
        
        // 注册EssentialsX适配器
        EssentialsXEconomyAdapter essentialsAdapter = new EssentialsXEconomyAdapter();
        availableAdapters.put("essentialsx", essentialsAdapter);
        
        plugin.getLogger().info("已注册 " + availableAdapters.size() + " 个经济系统适配器");
    }
    
    /**
     * 检测经济插件
     */
    private void detectEconomyPlugins() {
        plugin.getLogger().info("开始检测经济插件...");
        
        for (Map.Entry<String, EconomyAdapter> entry : availableAdapters.entrySet()) {
            EconomyAdapter adapter = entry.getValue();
            
            if (adapter.isAvailable()) {
                plugin.getLogger().info("检测到经济插件: " + adapter.getName() + 
                    " v" + adapter.getVersion() + " (优先级: " + adapter.getPriority() + ")");
            }
        }
    }
    
    /**
     * 选择最佳适配器
     */
    private void selectBestAdapter() {
        EconomyAdapter bestAdapter = null;
        int highestPriority = -1;
        
        for (EconomyAdapter adapter : availableAdapters.values()) {
            if (adapter.isAvailable() && adapter.getPriority() > highestPriority) {
                bestAdapter = adapter;
                highestPriority = adapter.getPriority();
            }
        }
        
        if (bestAdapter != null) {
            activeAdapter = bestAdapter;
            plugin.getLogger().info("选择经济系统适配器: " + bestAdapter.getName());
        }
    }
    
    /**
     * 启动定期检测任务
     */
    private void startDetectionTask() {
        if (detectionTask != null) {
            detectionTask.cancel();
        }
        
        detectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAdapterStatus();
            }
        };
        
        // 每30秒检测一次适配器状态
        detectionTask.runTaskTimerAsynchronously(plugin, 600L, 600L);
    }
    
    /**
     * 检查适配器状态
     */
    private void checkAdapterStatus() {
        if (activeAdapter != null && !activeAdapter.isAvailable()) {
            plugin.getLogger().warning("当前经济系统适配器 " + activeAdapter.getName() + " 不可用，尝试重新检测...");
            
            // 重新检测所有适配器
            detectEconomyPlugins();
            selectBestAdapter();
            
            if (activeAdapter != null) {
                plugin.getLogger().info("切换到新的经济系统适配器: " + activeAdapter.getName());
            } else {
                plugin.getLogger().warning("所有经济系统适配器都不可用");
            }
        }
    }
    
    /**
     * 关闭经济系统管理器
     */
    public void shutdown() {
        if (detectionTask != null) {
            detectionTask.cancel();
            detectionTask = null;
        }
        
        if (activeAdapter != null) {
            activeAdapter.clearError();
        }
        
        playerMoneyCache.clear();
        
        plugin.getLogger().info("经济系统管理器已关闭");
    }
    
    /**
     * 获取玩家传送费用
     */
    public double getTeleportCost(Player player) {
        if (!enabled || activeAdapter == null) {
            return 0.0;
        }
        
        // 检查免费权限
        if (player.hasPermission("rtp.free")) {
            return 0.0;
        }
        
        String permission = getPlayerPermission(player);
        return plugin.getConfigManager().getTeleportCost(permission);
    }
    
    /**
     * 检查玩家是否有足够的钱
     */
    public boolean hasEnoughMoney(Player player, double amount) {
        if (!enabled || activeAdapter == null) {
            return true;
        }
        
        return activeAdapter.hasEnoughMoney(player, amount);
    }
    
    /**
     * 扣除玩家金钱
     */
    public boolean withdrawMoney(Player player, double amount) {
        if (!enabled || activeAdapter == null) {
            return true;
        }
        
        boolean success = activeAdapter.withdrawMoney(player, amount);
        
        if (success) {
            // 清除缓存
            playerMoneyCache.remove(player.getUniqueId());
        }
        
        return success;
    }
    
    /**
     * 给玩家添加金钱（退款）
     */
    public boolean depositMoney(Player player, double amount) {
        if (!enabled || activeAdapter == null) {
            return false;
        }
        
        boolean success = activeAdapter.depositMoney(player, amount);
        
        if (success) {
            // 清除缓存
            playerMoneyCache.remove(player.getUniqueId());
        }
        
        return success;
    }
    
    /**
     * 获取玩家当前金钱
     */
    public double getPlayerMoney(Player player) {
        if (!enabled || activeAdapter == null) {
            return 0.0;
        }
        
        UUID playerId = player.getUniqueId();
        
        // 优先从缓存获取
        Double cachedMoney = playerMoneyCache.get(playerId);
        if (cachedMoney != null) {
            return cachedMoney;
        }
        
        // 从适配器获取
        double money = activeAdapter.getPlayerMoney(player);
        
        // 缓存结果
        playerMoneyCache.put(playerId, money);
        
        return money;
    }
    
    /**
     * 格式化金额显示
     */
    public String formatMoney(double amount) {
        if (!enabled || activeAdapter == null) {
            return String.valueOf(amount);
        }
        
        return activeAdapter.formatMoney(amount);
    }
    
    /**
     * 获取货币名称（复数）
     */
    public String getCurrencyName() {
        if (!enabled || activeAdapter == null) {
            return "硬币";
        }
        
        return activeAdapter.getCurrencyName();
    }
    
    /**
     * 获取货币名称（单数）
     */
    public String getCurrencyNameSingular() {
        if (!enabled || activeAdapter == null) {
            return "硬币";
        }
        
        return activeAdapter.getCurrencyNameSingular();
    }
    
    /**
     * 获取玩家权限等级
     */
    private String getPlayerPermission(Player player) {
        if (player.hasPermission("rtp.vipplus")) {
            return "rtp.vipplus";
        } else if (player.hasPermission("rtp.vip")) {
            return "rtp.vip";
        } else {
            return "rtp.use";
        }
    }
    
    /**
     * 检查经济系统是否启用
     */
    public boolean isEnabled() {
        return enabled && activeAdapter != null && activeAdapter.isAvailable();
    }
    
    /**
     * 获取当前适配器信息
     */
    public EconomyAdapter getActiveAdapter() {
        return activeAdapter;
    }
    
    /**
     * 获取当前适配器名称
     */
    public String getCurrentAdapterName() {
        if (activeAdapter == null) {
            return "无";
        }
        return activeAdapter.getName() + " v" + activeAdapter.getVersion();
    }
    
    /**
     * 检查特定适配器是否可用
     */
    public boolean isAdapterAvailable(String adapterName) {
        EconomyAdapter adapter = availableAdapters.get(adapterName.toLowerCase());
        return adapter != null && adapter.isAvailable();
    }
    
    /**
     * 检查是否支持特定功能
     */
    public boolean hasFeature(String featureName) {
        if (activeAdapter == null) {
            return false;
        }
        
        try {
            EconomyAdapter.Feature feature = EconomyAdapter.Feature.valueOf(featureName.toUpperCase());
            return activeAdapter.supportsFeature(feature);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 获取所有可用适配器
     */
    public Map<String, EconomyAdapter> getAvailableAdapters() {
        return new HashMap<>(availableAdapters);
    }
    
    /**
     * 手动切换适配器
     */
    public boolean switchAdapter(String adapterName) {
        EconomyAdapter adapter = availableAdapters.get(adapterName.toLowerCase());
        
        if (adapter != null && adapter.isAvailable()) {
            activeAdapter = adapter;
            plugin.getLogger().info("手动切换到适配器: " + adapter.getName());
            
            // 清除缓存
            playerMoneyCache.clear();
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取适配器状态信息
     */
    public String getStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("经济系统状态:\n");
        info.append("  启用状态: ").append(enabled ? "已启用" : "已禁用").append("\n");
        info.append("  当前适配器: ").append(activeAdapter != null ? activeAdapter.getName() : "无").append("\n");
        info.append("  可用适配器数量: ").append(availableAdapters.size()).append("\n");
        
        if (!availableAdapters.isEmpty()) {
            info.append("  适配器详情:\n");
            for (Map.Entry<String, EconomyAdapter> entry : availableAdapters.entrySet()) {
                EconomyAdapter adapter = entry.getValue();
                info.append("    ").append(adapter.getName())
                    .append(" v").append(adapter.getVersion())
                    .append(" (优先级: ").append(adapter.getPriority()).append(")")
                    .append(adapter.isAvailable() ? " - 可用" : " - 不可用")
                    .append("\n");
            }
        }
        
        return info.toString();
    }
    
    /**
     * 清除玩家金钱缓存
     */
    public void clearPlayerCache(UUID playerId) {
        playerMoneyCache.remove(playerId);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        playerMoneyCache.clear();
    }
}