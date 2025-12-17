package org.awaioi.randomtp.economy.adapter;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 经济系统适配器接口
 * 定义所有经济系统需要实现的标准接口
 */
public interface EconomyAdapter {
    
    /**
     * 获取适配器名称
     */
    String getName();
    
    /**
     * 获取适配器版本
     */
    String getVersion();
    
    /**
     * 获取对应的插件实例
     */
    Plugin getPlugin();
    
    /**
     * 检查经济系统是否可用
     */
    boolean isAvailable();
    
    /**
     * 检查玩家是否有足够的钱
     */
    boolean hasEnoughMoney(Player player, double amount);
    
    /**
     * 扣除玩家金钱
     */
    boolean withdrawMoney(Player player, double amount);
    
    /**
     * 给玩家添加金钱
     */
    boolean depositMoney(Player player, double amount);
    
    /**
     * 获取玩家当前金钱
     */
    double getPlayerMoney(Player player);
    
    /**
     * 格式化金额显示
     */
    String formatMoney(double amount);
    
    /**
     * 获取货币名称（复数）
     */
    String getCurrencyName();
    
    /**
     * 获取货币名称（单数）
     */
    String getCurrencyNameSingular();
    
    /**
     * 获取优先级（数值越高优先级越大）
     */
    int getPriority();
    
    /**
     * 检查是否支持特定功能
     */
    boolean supportsFeature(Feature feature);
    
    /**
     * 获取错误信息
     */
    String getLastError();
    
    /**
     * 重置错误信息
     */
    void clearError();
    
    /**
     * 经济系统功能枚举
     */
    enum Feature {
        TRANSACTION_LOGGING,    // 交易记录
        CURRENCY_FORMATTING,    // 货币格式化
        BULK_OPERATIONS,        // 批量操作
        TRANSACTION_ROLLBACK,   // 交易回滚
        CURRENCY_CONVERSION,    // 货币转换
        ADVANCED_PERMISSIONS,   // 高级权限
        BANK_OPERATIONS,        // 银行操作
        LOAN_SYSTEM,           // 贷款系统
        ECONOMY_STATISTICS,    // 经济统计
        MULTI_CURRENCY         // 多货币支持
    }
}