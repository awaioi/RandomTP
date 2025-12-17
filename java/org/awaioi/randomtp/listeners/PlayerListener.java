package org.awaioi.randomtp.listeners;

import java.util.UUID;

import org.awaioi.randomtp.RandomTP;
import org.awaioi.randomtp.teleport.TeleportManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 玩家事件监听器
 * 处理与传送相关的玩家事件
 */
public class PlayerListener implements Listener {
    
    private final RandomTP plugin;
    private final TeleportManager teleportManager;
    
    public PlayerListener(RandomTP plugin) {
        this.plugin = plugin;
        this.teleportManager = plugin.getTeleportManager();
    }
    
    /**
     * 玩家加入服务器事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 确保玩家数据已加载
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        
        // 发送欢迎消息（可选）
        // player.sendMessage(plugin.getConfigManager().getMessage("prefix") + 
        //     "欢迎使用随机传送插件！输入 /rtp help 查看帮助。");
    }
    
    /**
     * 玩家离开服务器事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // 取消传送任务
        if (teleportManager.hasActiveTeleport(uuid)) {
            teleportManager.cancelTeleport(player);
        }
        
        // 保存玩家数据
        plugin.getPlayerDataManager().savePlayerData();
    }
    
    /**
     * 玩家移动事件 - 取消传送延迟并退款
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // 检查玩家是否有正在进行的传送任务
        if (teleportManager.hasActiveTeleport(uuid)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            
            // 如果玩家移动了位置（不仅仅是视角），取消传送并退款
            if (from.getBlockX() != to.getBlockX() || 
                from.getBlockY() != to.getBlockY() || 
                from.getBlockZ() != to.getBlockZ()) {
                
                // 检查配置是否允许在移动时退款
                boolean shouldRefund = plugin.getConfigManager().isEconomyEnabled() && 
                                     plugin.getConfigManager().shouldRefundOnMove();
                
                // 取消传送并退款（如果配置允许）
                teleportManager.cancelTeleport(player, shouldRefund);
            }
        }
    }
    
    /**
     * 玩家受到伤害事件 - 不取消传送
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // 受到伤害时不取消传送，允许传送继续进行
    }
    
    /**
     * 玩家死亡事件 - 取消传送任务并退款
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // 检查玩家是否有正在进行的传送任务
        if (teleportManager.hasActiveTeleport(uuid)) {
            // 检查配置是否允许在死亡时退款
            boolean shouldRefund = plugin.getConfigManager().isEconomyEnabled() && 
                                 plugin.getConfigManager().shouldRefundOnDeath();
            
            // 取消传送并退款（如果配置允许）
            teleportManager.cancelTeleport(player, shouldRefund);
        }
        
        // 保存玩家数据
        plugin.getPlayerDataManager().savePlayerData();
    }
    
    /**
     * 玩家使用物品事件 - 不取消传送
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 与物品交互时不取消传送，允许传送继续进行
    }
    
    /**
     * 玩家攻击事件 - 不取消传送
     */
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 攻击行为时不取消传送，允许传送继续进行
    }
    
    /**
     * 玩家点击容器事件 - 不取消传送
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 点击容器时不取消传送，允许传送继续进行
    }
    
    /**
     * 玩家进入传送门事件 - 不取消传送
     */
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        // 使用传送门时不取消传送，允许传送继续进行
    }
    
    /**
     * 玩家切换世界事件 - 不取消传送
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // 切换世界时不取消传送，允许传送继续进行
    }
    
    /**
     * 玩家执行命令事件 - 不取消传送
     */
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // 执行命令时不取消传送，允许传送继续进行
    }
    
    /**
     * 玩家传送事件 - 取消随机传送任务并退款
     */
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // 检查玩家是否有正在进行的传送任务
        if (teleportManager.hasActiveTeleport(uuid)) {
            // 检查配置是否允许在其他方式传送时退款
            boolean shouldRefund = plugin.getConfigManager().isEconomyEnabled() && 
                                 plugin.getConfigManager().shouldRefundOnTeleport();
            
            // 取消随机传送并退款（如果配置允许）
            teleportManager.cancelTeleport(player, shouldRefund);
        }
    }
    
    /**
     * 玩家与实体交互事件 - 不取消传送
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // 与实体交互时不取消传送，允许传送继续进行
    }
    

}