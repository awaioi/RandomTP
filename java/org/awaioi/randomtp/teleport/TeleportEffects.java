package org.awaioi.randomtp.teleport;

import org.awaioi.randomtp.RandomTP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 传送效果管理器
 * 负责处理传送过程中的视觉效果、音效和倒计时
 */
public class TeleportEffects {
    
    private final RandomTP plugin;
    private final Map<UUID, Integer> activeCountdowns = new HashMap<>();
    private final Map<UUID, Integer> activeParticles = new HashMap<>();
    
    // 倒计时设置
    private static final int COUNTDOWN_SECONDS = 3;
    private static final long COUNTDOWN_INTERVAL = 20L; // 1秒间隔
    
    // 粒子效果设置
    private static final Particle ParticleType = Particle.PORTAL;
    private static final int PARTICLE_COUNT = 50;
    private static final double PARTICLE_OFFSET_X = 1.5;
    private static final double PARTICLE_OFFSET_Y = 2.0;
    private static final double PARTICLE_OFFSET_Z = 1.5;
    private static final double PARTICLE_SPEED = 0.5;
    
    public TeleportEffects(RandomTP plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 开始传送倒计时和效果
     * @param player 玩家
     * @param callback 倒计时完成后的回调
     */
    public void startTeleportSequence(Player player, Runnable callback) {
        if (activeCountdowns.containsKey(player.getUniqueId())) {
            return; // 已有活跃的倒计时
        }
        
        // 开始倒计时
        startCountdown(player, COUNTDOWN_SECONDS, callback);
        
        // 启动粒子效果
        startParticleEffect(player);
    }
    
    /**
     * 开始倒计时
     */
    private void startCountdown(Player player, int seconds, Runnable callback) {
        activeCountdowns.put(player.getUniqueId(), seconds);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                int currentSeconds = activeCountdowns.get(player.getUniqueId());
                
                if (currentSeconds <= 0) {
                    // 倒计时结束
                    activeCountdowns.remove(player.getUniqueId());
                    cancel();
                    
                    // 停止粒子效果
                    stopParticleEffect(player);
                    
                    // 播放传送音效
                    playTeleportSound(player);
                    
                    // 执行回调
                    if (callback != null) {
                        callback.run();
                    }
                    return;
                }
                
                // 更新倒计时
                activeCountdowns.put(player.getUniqueId(), currentSeconds - 1);
                
                // 播放倒计时音效
                playCountdownSound(player, currentSeconds);
                
                // 显示倒计时消息
                showCountdownMessage(player, currentSeconds);
                
                // 播放倒计时粒子效果
                playCountdownParticles(player, currentSeconds);
            }
        }.runTaskTimer(plugin, 0L, COUNTDOWN_INTERVAL);
    }
    
    /**
     * 播放倒计时音效
     */
    private void playCountdownSound(Player player, int seconds) {
        // 使用_note_harp音符盒声音模拟"噔噔噔"音效
        Sound sound;
        switch (seconds) {
            case 3:
                sound = Sound.BLOCK_NOTE_BLOCK_HARP;
                break;
            case 2:
                sound = Sound.BLOCK_NOTE_BLOCK_HARP;
                break;
            case 1:
                sound = Sound.BLOCK_NOTE_BLOCK_HARP;
                break;
            default:
                sound = Sound.BLOCK_NOTE_BLOCK_HARP;
        }
        
        player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
    }
    
    /**
     * 显示倒计时消息
     */
    private void showCountdownMessage(Player player, int seconds) {
        String message = plugin.getConfigManager().getMessage("teleport.countdown")
                .replace("%seconds%", String.valueOf(seconds));
        player.sendMessage(message);
    }
    
    /**
     * 播放倒计时粒子效果
     */
    private void playCountdownParticles(Player player, int seconds) {
        Location loc = player.getLocation();
        
        // 根据剩余秒数调整粒子颜色
        Particle.DustOptions dustOptions = null;
        switch (seconds) {
            case 3:
                dustOptions = new Particle.DustOptions(Color.RED, 1.0F);
                break;
            case 2:
                dustOptions = new Particle.DustOptions(Color.YELLOW, 1.0F);
                break;
            case 1:
                dustOptions = new Particle.DustOptions(Color.GREEN, 1.0F);
                break;
        }
        
        if (dustOptions != null) {
            player.getWorld().spawnParticle(Particle.REDSTONE, loc, 30, 
                    PARTICLE_OFFSET_X, PARTICLE_OFFSET_Y, PARTICLE_OFFSET_Z, 
                    1.0, dustOptions);
        }
    }
    
    /**
     * 开始粒子效果
     */
    private void startParticleEffect(Player player) {
        activeParticles.put(player.getUniqueId(), 0);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeCountdowns.containsKey(player.getUniqueId())) {
                    // 倒计时已结束，停止粒子效果
                    activeParticles.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation().clone().add(0, 1, 0);
                
                // 传送门粒子效果
                player.getWorld().spawnParticle(ParticleType, loc, PARTICLE_COUNT, 
                        PARTICLE_OFFSET_X, PARTICLE_OFFSET_Y, PARTICLE_OFFSET_Z, 
                        PARTICLE_SPEED);
                
                // 额外的魔法粒子
                player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 20, 
                        1.0, 1.5, 1.0, 1.0);
                
                // 神秘粒子
                player.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 15, 
                        0.5, 1.0, 0.5, 1.0);
            }
        }.runTaskTimer(plugin, 0L, 5L); // 每0.25秒更新一次
    }
    
    /**
     * 停止粒子效果
     */
    private void stopParticleEffect(Player player) {
        activeParticles.remove(player.getUniqueId());
    }
    
    /**
     * 播放传送成功音效
     */
    private void playTeleportSound(Player player) {
        // 使用Enderman传送音效
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0F, 1.0F);
        
        // 额外的成功音效
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.5F, 1.5F);
    }
    
    /**
     * 播放传送到达粒子效果
     */
    public void playArrivalEffect(Player player) {
        Location loc = player.getLocation();
        
        // 到达爆炸效果
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 100, 
                PARTICLE_OFFSET_X * 1.5, PARTICLE_OFFSET_Y * 1.5, PARTICLE_OFFSET_Z * 1.5, 
                PARTICLE_SPEED * 1.5);
        
        // 魔法光环效果
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, 0.5, 0), 50, 
                2.0, 0.5, 2.0, 2.0);
        
        // 成功粒子
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 30, 
                1.0, 1.0, 1.0, 1.0);
        
        // 播放到达音效
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 2.0F);
    }
    
    /**
     * 取消玩家的所有效果
     */
    public void cancelEffects(Player player) {
        activeCountdowns.remove(player.getUniqueId());
        activeParticles.remove(player.getUniqueId());
    }
    
    /**
     * 检查玩家是否有活跃的倒计时
     */
    public boolean hasActiveCountdown(Player player) {
        return activeCountdowns.containsKey(player.getUniqueId());
    }
    
    /**
     * 获取倒计时剩余秒数
     */
    public int getRemainingCountdown(Player player) {
        return activeCountdowns.getOrDefault(player.getUniqueId(), 0);
    }
}