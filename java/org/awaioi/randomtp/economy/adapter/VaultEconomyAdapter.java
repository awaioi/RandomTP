package org.awaioi.randomtp.economy.adapter;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault经济系统适配器
 * 支持所有基于Vault的经济插件（EssentialsX、Xconomy等）
 */
public class VaultEconomyAdapter implements EconomyAdapter {
    
    private final Economy economy;
    private final Plugin plugin;
    private final String pluginName;
    private final String errorMessage;
    private boolean available;
    
    public VaultEconomyAdapter() {
        this.plugin = null;
        this.economy = getEconomy();
        this.pluginName = getVaultPluginName();
        this.errorMessage = null;
        this.available = (economy != null);
    }
    
    @Override
    public String getName() {
        return "Vault (" + pluginName + ")";
    }
    
    @Override
    public String getVersion() {
        return economy != null ? economy.getName() : "Unknown";
    }
    
    @Override
    public Plugin getPlugin() {
        return plugin;
    }
    
    @Override
    public boolean isAvailable() {
        return available && economy != null;
    }
    
    @Override
    public boolean hasEnoughMoney(Player player, double amount) {
        if (!isAvailable()) {
            return true; // 经济系统不可用时默认允许
        }
        
        try {
            return economy.has(player, amount);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RandomTP] Vault hasEnoughMoney error: " + e.getMessage());
            return true;
        }
    }
    
    @Override
    public boolean withdrawMoney(Player player, double amount) {
        if (!isAvailable()) {
            return true;
        }
        
        try {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response.transactionSuccess();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RandomTP] Vault withdrawMoney error: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean depositMoney(Player player, double amount) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            EconomyResponse response = economy.depositPlayer(player, amount);
            return response.transactionSuccess();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RandomTP] Vault depositMoney error: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public double getPlayerMoney(Player player) {
        if (!isAvailable()) {
            return 0.0;
        }
        
        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RandomTP] Vault getPlayerMoney error: " + e.getMessage());
            return 0.0;
        }
    }
    
    @Override
    public String formatMoney(double amount) {
        if (!isAvailable()) {
            return String.valueOf(amount);
        }
        
        try {
            return economy.format(amount);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RandomTP] Vault formatMoney error: " + e.getMessage());
            return String.valueOf(amount);
        }
    }
    
    @Override
    public String getCurrencyName() {
        if (!isAvailable()) {
            return "硬币";
        }
        
        try {
            String currencyName = economy.currencyNamePlural();
            return currencyName != null && !currencyName.isEmpty() ? currencyName : "硬币";
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RandomTP] Vault getCurrencyName error: " + e.getMessage());
            return "硬币";
        }
    }
    
    @Override
    public String getCurrencyNameSingular() {
        if (!isAvailable()) {
            return "硬币";
        }
        
        try {
            String currencyName = economy.currencyNameSingular();
            return currencyName != null && !currencyName.isEmpty() ? currencyName : "硬币";
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RandomTP] Vault getCurrencyNameSingular error: " + e.getMessage());
            return "硬币";
        }
    }
    
    @Override
    public int getPriority() {
        return 100; // Vault适配器高优先级
    }
    
    @Override
    public boolean supportsFeature(Feature feature) {
        switch (feature) {
            case TRANSACTION_LOGGING:
                return false; // Vault本身不支持，需要具体插件
            case CURRENCY_FORMATTING:
                return true;
            case BULK_OPERATIONS:
                return true;
            case TRANSACTION_ROLLBACK:
                return false; // Vault不支持交易回滚
            case CURRENCY_CONVERSION:
                return false;
            case ADVANCED_PERMISSIONS:
                return true;
            case BANK_OPERATIONS:
                return true;
            case LOAN_SYSTEM:
                return false;
            case ECONOMY_STATISTICS:
                return false;
            case MULTI_CURRENCY:
                return false;
            default:
                return false;
        }
    }
    
    @Override
    public String getLastError() {
        return errorMessage;
    }
    
    @Override
    public void clearError() {
        // Vault适配器不需要错误重置
    }
    
    /**
     * 获取Vault经济对象
     */
    private Economy getEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = 
            Bukkit.getServicesManager().getRegistration(Economy.class);
        
        if (economyProvider != null) {
            return economyProvider.getProvider();
        }
        
        return null;
    }
    
    /**
     * 获取Vault插件名称
     */
    private String getVaultPluginName() {
        RegisteredServiceProvider<Economy> economyProvider = 
            Bukkit.getServicesManager().getRegistration(Economy.class);
        
        if (economyProvider != null) {
            Plugin providerPlugin = economyProvider.getPlugin();
            return providerPlugin != null ? providerPlugin.getName() : "Unknown";
        }
        
        return "None";
    }
}