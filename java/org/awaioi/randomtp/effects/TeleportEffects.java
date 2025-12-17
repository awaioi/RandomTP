package org.awaioi.randomtp.effects;

import org.awaioi.randomtp.RandomTP;
import org.awaioi.randomtp.config.ConfigManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 传送效果管理器
 * 负责处理传送前后的视觉效果和玩家效果
 */
public class TeleportEffects {
    
    private final RandomTP plugin;
    
    public TeleportEffects(RandomTP plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 启动传送序列（包含效果和倒计时）
     */
    public void startTeleportSequence(Player player, Runnable callback) {
        ConfigManager config = plugin.getConfigManager();
        int delay = config.getTeleportDelay();
        
        // 显示倒计时消息
        player.sendMessage(config.getMessage("prefix") + 
            ChatColor.YELLOW + "传送将在 " + delay + " 秒后开始，请不要移动...");
        
        // 启动倒计时效果
        startCountdownEffect(player, delay, callback);
    }
    
    /**
     * 开始倒计时效果
     */
    private void startCountdownEffect(Player player, int totalSeconds, Runnable callback) {
        new BukkitRunnable() {
            int secondsLeft = totalSeconds;
            
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }
                
                if (secondsLeft <= 0) {
                    this.cancel();
                    // 执行传送
                    callback.run();
                    return;
                }
                
                // 每秒显示倒计时
                if (secondsLeft <= 5 || secondsLeft % 5 == 0) {
                    player.sendMessage(plugin.getConfigManager().getMessage("prefix") + 
                        ChatColor.AQUA + "传送倒计时: " + secondsLeft + " 秒");
                }
                
                // 播放倒计时音效
                playCountdownSound(player);
                
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * 播放倒计时音效
     */
    private void playCountdownSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 1.0F);
        } catch (Exception e) {
            // 忽略音效错误
        }
    }
    
    /**
     * 播放传送到达效果
     */
    public void playArrivalEffect(Player player) {
        Location location = player.getLocation();
        
        // 播放到达音效
        try {
            player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
        } catch (Exception e) {
            // 忽略音效错误
        }
        
        // 播放到达粒子效果
        try {
            player.getWorld().spawnParticle(Particle.PORTAL, location, 50, 2, 2, 2, 1);
        } catch (Exception e) {
            // 忽略粒子错误
        }
    }
    
    /**
     * 取消玩家所有传送相关效果
     */
    public void cancelEffects(Player player) {
        // 移除所有传送相关效果
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        
        // 可以添加更多效果清理逻辑
    }
}