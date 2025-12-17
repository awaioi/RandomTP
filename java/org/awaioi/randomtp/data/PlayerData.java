package org.awaioi.randomtp.data;

import java.util.UUID;

/**
 * 玩家数据类
 * 存储单个玩家的传送相关数据
 */
public class PlayerData {
    
    private final UUID uuid;
    private long lastTeleport; // 上次传送时间戳
    private int teleportCount; // 传送次数
    private double totalCost; // 总传送费用
    
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.lastTeleport = 0;
        this.teleportCount = 0;
        this.totalCost = 0.0;
    }
    
    /**
     * 获取玩家UUID
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * 获取上次传送时间
     */
    public long getLastTeleport() {
        return lastTeleport;
    }
    
    /**
     * 设置上次传送时间
     */
    public void setLastTeleport(long lastTeleport) {
        this.lastTeleport = lastTeleport;
    }
    
    /**
     * 获取传送次数
     */
    public int getTeleportCount() {
        return teleportCount;
    }
    
    /**
     * 设置传送次数
     */
    public void setTeleportCount(int teleportCount) {
        this.teleportCount = teleportCount;
    }
    
    /**
     * 增加传送次数
     */
    public void incrementTeleportCount() {
        this.teleportCount++;
    }
    
    /**
     * 获取总费用
     */
    public double getTotalCost() {
        return totalCost;
    }
    
    /**
     * 设置总费用
     */
    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    
    /**
     * 添加费用
     */
    public void addTotalCost(double cost) {
        this.totalCost += cost;
    }
    
    /**
     * 获取玩家名称（如果在线）
     */
    public String getPlayerName() {
        org.bukkit.entity.Player player = org.awaioi.randomtp.RandomTP.getInstance()
            .getServer()
            .getPlayer(uuid);
        return player != null ? player.getName() : "Unknown";
    }
}