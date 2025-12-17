package org.awaioi.randomtp.teleport;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.awaioi.randomtp.RandomTP;
import org.awaioi.randomtp.economy.EconomySystemManager;
import org.awaioi.randomtp.effects.TeleportEffects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 传送管理器
 * 负责处理随机传送的核心逻辑，包括坐标生成、安全验证、传送执行等
 */
public class TeleportManager {
    
    private final RandomTP plugin;
    private final Map<UUID, BukkitTask> teleportTasks;
    private final Random random;
    private final TeleportEffects teleportEffects;
    private final EconomySystemManager economySystemManager;
    
    public TeleportManager(RandomTP plugin) {
        this.plugin = plugin;
        this.teleportTasks = new HashMap<>();
        this.random = new Random();
        this.teleportEffects = new TeleportEffects(plugin);
        this.economySystemManager = plugin.getEconomySystemManager();
    }
    
    /**
     * 执行随机传送
     */
    public void performTeleport(Player player, boolean free, boolean bypassCooldown) {
        // 检查冷却
        if (!bypassCooldown && !canTeleport(player)) {
            long remainingTime = plugin.getPlayerDataManager().getRemainingCooldown(player.getUniqueId());
            String message = plugin.getConfigManager().getFormattedMessage("cooldown", "time", String.valueOf(remainingTime));
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") + message);
            return;
        }
        
        // 检查经济
        if (!free && !checkAndDeductEconomy(player)) {
            return;
        }
        
        // 开始传送延迟
        startTeleportDelay(player, free, bypassCooldown);
    }
    
    /**
     * 开始传送延迟倒计时
     */
    private void startTeleportDelay(Player player, boolean free, boolean bypassCooldown) {
        // 检查是否有正在进行的传送任务
        if (hasActiveTeleport(player.getUniqueId())) {
            // 如果已有传送任务，显示提示消息
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") + 
                ChatColor.YELLOW + "你已有传送任务正在进行中。");
            return;
        }
        
        // 启动新的传送序列（包含倒计时和效果）
        startTeleportSequence(player, () -> {
            executeTeleport(player, free, bypassCooldown);
        });
    }
    
    /**
     * 启动传送序列（包含效果和倒计时）
     */
    private void startTeleportSequence(Player player, Runnable callback) {
        // 使用新的效果系统
        teleportEffects.startTeleportSequence(player, callback);
    }
    
    /**
     * 执行实际传送
     */
    private void executeTeleport(Player player, boolean free, boolean bypassCooldown) {
        // 异步寻找安全位置
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location safeLocation = findSafeRandomLocation(player);
            
            // 同步执行传送
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (safeLocation != null && player.isOnline() && !player.isDead()) {
                    // 执行传送
                    player.teleport(safeLocation);
                    
                    // 播放传送到达效果
                    teleportEffects.playArrivalEffect(player);
                    
                    // 添加短暂无敌BUFF
                    addInvincibilityBuff(player);
                    
                    // 更新玩家数据
                    updatePlayerData(player, free);
                    
                    // 显示成功消息
                    String successMessage = plugin.getConfigManager().getMessage("teleported");
                    
                    // 如果不是免费传送，显示本次花费
                    if (!free) {
                        double currentCost = plugin.getConfigManager().getTeleportCost(player);
                        if (currentCost > 0) {
                            String formattedCost = economySystemManager.formatMoney(currentCost);
                            successMessage += ChatColor.GOLD + " 本次花费: " + formattedCost;
                        }
                    }
                    
                    player.sendMessage(plugin.getConfigManager().getMessage("prefix") + successMessage);
                    
                    plugin.getLogger().info(player.getName() + " 随机传送到 " + 
                        safeLocation.getBlockX() + ", " + safeLocation.getBlockY() + ", " + 
                        safeLocation.getBlockZ());
                    
                } else {
                    // 传送失败
                    player.sendMessage(plugin.getConfigManager().getMessage("prefix") + 
                        ChatColor.RED + "无法找到安全传送位置，请稍后重试");
                    plugin.getLogger().warning(player.getName() + " 随机传送失败：无法找到安全位置");
                }
            });
        });
    }
    
    /**
     * 寻找安全的随机位置
     */
    private Location findSafeRandomLocation(Player player) {
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        
        // 第一阶段：标准重试机制
        for (int attempt = 0; attempt < plugin.getConfigManager().getMaxTries(); attempt++) {
            Location safeLocation = generateAndCheckLocation(player, playerLocation, world, false);
            if (safeLocation != null) {
                return safeLocation;
            }
        }
        
        // 第二阶段：降低标准重试（减少范围）
        for (int attempt = 0; attempt < 5; attempt++) {
            Location safeLocation = generateAndCheckLocation(player, playerLocation, world, true);
            if (safeLocation != null) {
                return safeLocation;
            }
        }
        
        // 第三阶段：极低标准重试（非常小的范围）
        return findAnySafeLocation(player);
    }
    
    /**
     * 生成并检查位置
     */
    private Location generateAndCheckLocation(Player player, Location playerLocation, World world, boolean reducedRange) {
        int range = reducedRange ? plugin.getConfigManager().getTeleportRange() / 2 : plugin.getConfigManager().getTeleportRange();
        
        // 生成随机坐标
        int x = playerLocation.getBlockX() + random.nextInt(range * 2) - range;
        int z = playerLocation.getBlockZ() + random.nextInt(range * 2) - range;
        
        // 获取最高固体方块位置
        int y = world.getHighestBlockYAt(x, z);
        
        // 检查Y轴限制
        if (y < plugin.getConfigManager().getMinY() || y > plugin.getConfigManager().getMaxY()) {
            return null;
        }
        
        // 检查周围区块是否已加载
        if (!isChunkLoaded(world, x, z)) {
            return null;
        }
        
        Location testLocation = new Location(world, x + 0.5, y + 1, z + 0.5);
        
        // 检查安全位置
        if (isSafeLocation(testLocation)) {
            return testLocation;
        }
        
        return null;
    }
    
    /**
     * 检查区块是否已加载
     */
    private boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
    }
    
    /**
     * 寻找任何安全位置（降低标准）
     */
    private Location findAnySafeLocation(Player player) {
        int maxTries = plugin.getConfigManager().getMaxTries() * 2;
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        
        for (int attempt = 0; attempt < maxTries; attempt++) {
            int x = playerLocation.getBlockX() + random.nextInt(2000) - 1000;
            int z = playerLocation.getBlockZ() + random.nextInt(2000) - 1000;
            
            int y = world.getHighestBlockYAt(x, z);
            Location testLocation = new Location(world, x + 0.5, y, z + 0.5);
            
            // 只检查基本安全要求
            if (isBasicSafeLocation(testLocation)) {
                return testLocation;
            }
        }
        
        return null;
    }
    
    /**
     * 检查位置是否安全
     */
    private boolean isSafeLocation(Location location) {
        if (!plugin.getConfigManager().shouldFindSafeLocation()) {
            return true;
        }
        
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        Block above = block.getRelative(BlockFace.UP);
        Block above2 = block.getRelative(BlockFace.UP, 2);
        
        // 检查下方是否为固体方块
        if (!below.getType().isSolid()) {
            return false;
        }
        
        // 检查当前方块是否为空气
        if (!isAir(block.getType())) {
            return false;
        }
        
        // 检查上方1格是否为空气
        if (!isAir(above.getType())) {
            return false;
        }
        
        // 检查上方2格是否为空气（确保玩家可以站立）
        if (!isAir(above2.getType())) {
            return false;
        }
        
        // 避免水中传送
        if (plugin.getConfigManager().shouldAvoidWater() && isWaterNearby(location, 2)) {
            return false;
        }
        
        // 避免岩浆传送
        if (plugin.getConfigManager().shouldAvoidLava() && isLavaNearby(location, 2)) {
            return false;
        }
        
        // 检查周围危险方块
        if (hasDangerousBlocksNearby(location, 2)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查是否为空气方块
     */
    private boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }
    
    /**
     * 检查附近是否有水域
     */
    private boolean isWaterNearby(Location location, int radius) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    if (isWater(block.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 检查附近是否有岩浆
     */
    private boolean isLavaNearby(Location location, int radius) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    if (block.getType() == Material.LAVA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 检查附近是否有危险方块
     */
    private boolean hasDangerousBlocksNearby(Location location, int radius) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // 危险方块列表
        Material[] dangerousBlocks = {
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.OBSIDIAN,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.END_CRYSTAL
        };
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    for (Material dangerous : dangerousBlocks) {
                        if (block.getType() == dangerous) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 检查是否为水
     */
    private boolean isWater(Material material) {
        return material == Material.WATER;
    }
    
    /**
     * 检查基本安全要求
     */
    private boolean isBasicSafeLocation(Location location) {
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        
        return below.getType().isSolid() && 
               (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR);
    }
    
    /**
     * 检查是否为水域
     */
    private boolean isWaterArea(Location location) {
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        
        return block.isLiquid() || below.isLiquid();
    }
    
    /**
     * 检查是否为岩浆区域
     */
    private boolean isLavaArea(Location location) {
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        
        return block.getType() == Material.LAVA || below.getType() == Material.LAVA;
    }
    
    /**
     * 检查玩家是否可以传送
     */
    public boolean canTeleport(Player player) {
        return !plugin.getPlayerDataManager().isInCooldown(player.getUniqueId());
    }
    
    private boolean checkAndDeductEconomy(Player player) {
        if (!plugin.getConfigManager().isEconomyEnabled()) {
            return true; // 经济系统未启用，允许传送
        }

        double cost = plugin.getConfigManager().getTeleportCost(player);
        if (cost <= 0) {
            return true; // 费用为0，允许传送
        }

        EconomySystemManager economyManager = plugin.getEconomySystemManager();
        if (economyManager == null) {
            player.sendMessage(ChatColor.RED + "经济系统未正确配置，无法进行传送。");
            return false;
        }

        if (!economyManager.hasEnoughMoney(player, cost)) {
            String formattedCost = economyManager.formatMoney(cost);
            player.sendMessage(ChatColor.RED + "你需要 " + formattedCost + " 才能传送，但你余额不足。");
            return false;
        }

        if (!economyManager.withdrawMoney(player, cost)) {
            player.sendMessage(ChatColor.RED + "扣费失败，无法传送。");
            return false;
        }

        return true;
    }
    
    /**
     * 更新玩家数据
     */
    private void updatePlayerData(Player player, boolean free) {
        UUID uuid = player.getUniqueId();
        
        // 更新传送时间
        plugin.getPlayerDataManager().updateTeleportTime(uuid);
        
        // 保存数据
        plugin.getPlayerDataManager().savePlayerData();
    }
    
    /**
     * 取消玩家传送
     */
    public void cancelTeleport(Player player) {
        cancelTeleport(player, false); // 默认不是退款情况
    }
    
    /**
     * 取消玩家传送（带退款选项）
     * @param player 玩家
     * @param shouldRefund 是否应该退款（玩家移动或执行取消操作时为true）
     */
    public void cancelTeleport(Player player, boolean shouldRefund) {
        UUID uuid = player.getUniqueId();
        
        if (teleportTasks.containsKey(uuid)) {
            teleportTasks.get(uuid).cancel();
            teleportTasks.remove(uuid);
        }
        
        // 取消效果系统中的效果
        teleportEffects.cancelEffects(player);
        
        // 退款逻辑 - 只在玩家移动或执行其他取消操作时退款
        if (shouldRefund && plugin.getConfigManager().isEconomyEnabled()) {
            EconomySystemManager economySystemManager = plugin.getEconomySystemManager();
            if (economySystemManager != null && economySystemManager.isEnabled()) {
                double cost = plugin.getConfigManager().getTeleportCost(player);
                if (cost > 0 && economySystemManager.depositMoney(player, cost)) {
                    String formattedRefund = economySystemManager.formatMoney(cost);
                    player.sendMessage(ChatColor.GREEN + "已退还传送费用: " + formattedRefund);
                } else if (cost > 0) {
                    String formattedCost = economySystemManager.formatMoney(cost);
                    player.sendMessage(ChatColor.YELLOW + "退款失败，请联系管理员。费用: " + formattedCost);
                }
            }
        }
        
        player.sendMessage(plugin.getConfigManager().getMessage("prefix") + 
            plugin.getConfigManager().getMessage("teleport-cancelled"));
    }
    
    /**
     * 取消所有传送任务
     */
    public void cancelAllTeleports() {
        for (BukkitTask task : teleportTasks.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        teleportTasks.clear();
    }
    
    /**
     * 绕过冷却传送
     */
    public void bypassCooldownTeleport(Player player) {
        plugin.getPlayerDataManager().bypassCooldown(player.getUniqueId());
        performTeleport(player, true, true);
    }
    
    /**
     * 获取传送效果管理器
     */
    public TeleportEffects getTeleportEffects() {
        return teleportEffects;
    }
    
    /**
     * 免费传送
     */
    public void freeTeleport(Player player) {
        performTeleport(player, true, false);
    }
    
    /**
     * 获取玩家传送任务
     */
    public BukkitTask getTeleportTask(UUID uuid) {
        return teleportTasks.get(uuid);
    }
    
    /**
     * 检查玩家是否有正在进行的传送
     */
    public boolean hasActiveTeleport(UUID uuid) {
        return teleportTasks.containsKey(uuid);
    }
    
    /**
     * 添加短暂无敌BUFF
     */
    private void addInvincibilityBuff(Player player) {
        int buffDuration = 5; // 5秒无敌时间
        
        // 添加抗性提升效果（等级1，持续5秒）
        PotionEffect resistance = new PotionEffect(
            PotionEffectType.DAMAGE_RESISTANCE,
            buffDuration * 20, // 转换为tick
            1, // 等级1（75%伤害减免）
            false, // 不显示粒子
            false, // 不显示图标
            true // 隐藏效果
        );
        
        player.addPotionEffect(resistance);
        
        // 记录日志
        plugin.getLogger().info("为玩家 " + player.getName() + " 添加了 " + buffDuration + " 秒无敌BUFF");
        
        // 安排移除效果的任务（确保效果不会过早消失）
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    plugin.getLogger().info("玩家 " + player.getName() + " 的无敌BUFF已移除");
                }
            }
        }.runTaskLater(plugin, buffDuration * 20L);
    }
    
    /**
     * 取消所有传送任务并清理玩家BUFF
     */
    public void cancelAllTeleportsAndCleanup() {
        cancelAllTeleports();
        
        // 清理所有在线玩家的无敌BUFF
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        }
    }
}