package org.awaioi.randomtp.economy;

import org.awaioi.randomtp.RandomTP;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * 经济系统管理器
 * 负责与Vault API集成，处理传送费用
 */
public class EconomyManager {
    
    private final RandomTP plugin;
    private Economy economy;
    private boolean enabled;
    private boolean vaultAvailable;
    
    public EconomyManager(RandomTP plugin) {
        this.plugin = plugin;
        this.enabled = false;
        this.vaultAvailable = false;
    }
    
    /**
     * 初始化经济系统
     */
    public void initialize() {
        if (!plugin.getConfigManager().isEconomyEnabled()) {
            plugin.getLogger().info("经济系统已禁用");
            return;
        }
        
        // 检查Vault插件
        if (!isVaultAvailable()) {
            plugin.getLogger().warning("Vault插件未找到，经济系统功能不可用");
            enabled = false;
            return;
        }
        
        // 获取经济服务
        if (!setupEconomy()) {
            plugin.getLogger().warning("无法初始化经济系统，经济功能将被禁用");
            enabled = false;
            return;
        }
        
        enabled = true;
        plugin.getLogger().info("经济系统初始化成功");
    }
    

    
    /**
     * 设置经济系统
     */
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = 
            Bukkit.getServicesManager().getRegistration(Economy.class);
        
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
            vaultAvailable = true;
            return economy != null;
        }
        
        return false;
    }
    
    /**
     * 检查玩家是否有足够的钱
     */
    public boolean hasEnoughMoney(Player player, double amount) {
        if (!enabled || !vaultAvailable || economy == null) {
            return true; // 经济系统不可用时默认允许传送
        }
        
        return economy.has(player, amount);
    }
    
    /**
     * 扣除玩家金钱
     */
    public boolean withdrawMoney(Player player, double amount) {
        if (!enabled || !vaultAvailable || economy == null) {
            return true; // 经济系统不可用时默认允许传送
        }
        
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }
    
    /**
     * 给玩家添加金钱（用于退款等功能）
     */
    public boolean depositMoney(Player player, double amount) {
        if (!enabled || !vaultAvailable || economy == null) {
            return false;
        }
        
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }
    
    /**
     * 获取玩家当前金钱
     */
    public double getPlayerMoney(Player player) {
        if (!enabled || !vaultAvailable || economy == null) {
            return 0.0;
        }
        
        return economy.getBalance(player);
    }
    
    /**
     * 获取玩家传送费用
     */
    public double getTeleportCost(Player player) {
        if (!enabled) {
            return 0.0; // 经济系统禁用时免费
        }
        
        String permission = getPlayerPermission(player);
        return plugin.getConfigManager().getTeleportCost(permission);
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
     * 格式化金额显示
     */
    public String formatMoney(double amount) {
        if (!enabled || !vaultAvailable || economy == null) {
            return String.valueOf(amount);
        }
        
        return economy.format(amount);
    }
    
    /**
     * 检查经济系统是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 检查Vault是否可用
     */
    public boolean isVaultAvailable() {
        return vaultAvailable;
    }
    
    /**
     * 获取经济对象（用于高级操作）
     */
    public Economy getEconomy() {
        return economy;
    }
    
    /**
     * 获取货币名称
     */
    public String getCurrencyName() {
        if (!enabled || !vaultAvailable || economy == null) {
            return "硬币";
        }
        
        String currencyName = economy.currencyNamePlural();
        return currencyName != null && !currencyName.isEmpty() ? currencyName : "硬币";
    }
    
    /**
     * 获取货币名称（单数形式）
     */
    public String getCurrencyNameSingular() {
        if (!enabled || !vaultAvailable || economy == null) {
            return "硬币";
        }
        
        String currencyName = economy.currencyNameSingular();
        return currencyName != null && !currencyName.isEmpty() ? currencyName : "硬币";
    }
}