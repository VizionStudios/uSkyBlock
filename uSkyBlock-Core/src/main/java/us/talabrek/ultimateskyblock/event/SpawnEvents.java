package us.talabrek.ultimateskyblock.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Monster;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.EntityUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static us.talabrek.ultimateskyblock.util.I18nUtil.tr;

/**
 * Responsible for controlling spawns on uSkyBlock islands.
 */
public class SpawnEvents implements Listener {
    private static final Set<Action> RIGHT_CLICKS = new HashSet<>(Arrays.asList(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK));
    private static final Set<CreatureSpawnEvent.SpawnReason> PLAYER_INITIATED = new HashSet<>(Arrays.asList(
            CreatureSpawnEvent.SpawnReason.BREEDING,
            CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM, CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN,
            CreatureSpawnEvent.SpawnReason.BUILD_WITHER
    ));
    private static final Map<EntityType, Set<Material>> FODDER = new HashMap<>();
    static {
        FODDER.put(EntityType.PIG, Collections.singleton(Material.CARROT_ITEM));
        FODDER.put(EntityType.COW, Collections.singleton(Material.WHEAT));
        FODDER.put(EntityType.SHEEP, Collections.singleton(Material.WHEAT));
        FODDER.put(EntityType.CHICKEN, Collections.singleton(Material.SEEDS));
        FODDER.put(EntityType.OCELOT, Collections.singleton(Material.RAW_FISH));
        FODDER.put(EntityType.WOLF, Collections.singleton(Material.RAW_BEEF));
        FODDER.put(EntityType.HORSE, new HashSet<>(Arrays.asList(Material.GOLDEN_APPLE, Material.GOLDEN_CARROT)));
        FODDER.put(EntityType.RABBIT, new HashSet<>(Arrays.asList(Material.CARROT, Material.GOLDEN_CARROT, Material.YELLOW_FLOWER)));
    }
    private final uSkyBlock plugin;

    public SpawnEvents(uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFeedEvent(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null || !plugin.isSkyWorld(player.getWorld())) {
            return; // Bail out, we don't care
        }
        if (Animals.class.isAssignableFrom(event.getRightClicked().getType().getEntityClass()) && player.getItemInHand() != null) {
            if (isFodder(event.getRightClicked(), player.getItemInHand())) {
                checkLimits(event, event.getRightClicked().getType(), player.getLocation());
                if (event.isCancelled()) {
                    plugin.notifyPlayer(player, tr("\u00a7cYou have reached your spawn-limit for your island."));
                }
            }
        }
    }

    private boolean isFodder(Entity entity, ItemStack item) {
        if (entity == null || entity.getType() == null || item == null && item.getType() == null) {
            return false;
        }
        Set<Material> acceptedFodder = FODDER.get(entity.getType());
        return acceptedFodder != null && acceptedFodder.contains(item.getType());
    }

    @EventHandler
    public void onSpawnEggEvent(PlayerInteractEvent event) {
        Player player = event != null ? event.getPlayer() : null;
        if (player == null || !plugin.isSkyWorld(player.getWorld())) {
            return; // Bail out, we don't care
        }
        ItemStack item = event.getItem();
        if (RIGHT_CLICKS.contains(event.getAction()) && item != null && item.getType() == Material.MONSTER_EGG && item.getData() instanceof SpawnEgg) {
            if (!plugin.playerIsOnIsland(player)) {
                event.setCancelled(true);
                plugin.notifyPlayer(player, tr("\u00a7eYou can only use spawn-eggs on your own island."));
                return;
            }
            SpawnEgg spawnEgg = (SpawnEgg) item.getData();
            checkLimits(event, spawnEgg.getSpawnedType(), player.getLocation());
            if (event.isCancelled()) {
                plugin.notifyPlayer(player, tr("\u00a7cYou have reached your spawn-limit for your island."));
                event.setUseItemInHand(Event.Result.DENY);
                event.setUseInteractedBlock(Event.Result.DENY);
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event == null || event.getLocation() == null || !plugin.isSkyWorld(event.getLocation().getWorld())) {
            return; // Bail out, we don't care
        }
        checkLimits(event, event.getEntity().getType(), event.getLocation());
    }

    private void checkLimits(Cancellable event, EntityType entityType, Location location) {
        if (entityType == null) {
            return; // Only happens on "other-plugins", i.e. EchoPet
        }
        String islandName = WorldGuardHandler.getIslandNameAt(location);
        if (islandName == null) {
            event.setCancelled(true); // Only allow spawning on active islands...
            return;
        }
        if (entityType.getEntityClass().isAssignableFrom(Ghast.class)) {
            // Disallow ghasts for now...
            event.setCancelled(true);
            return;
        }
        IslandInfo islandInfo = plugin.getIslandInfo(islandName);
        if (islandInfo == null) {
            // Disallow spawns on inactive islands
            event.setCancelled(true);
            return;
        }
        List<Creature> creatures = WorldGuardHandler.getCreaturesInRegion(plugin.getWorld(), WorldGuardHandler.getIslandRegionAt(islandInfo.getIslandLocation()));
        if (Monster.class.isAssignableFrom(entityType.getEntityClass())) {
            long currentlySpawned = EntityUtil.getMonsters(creatures).size();
            if (currentlySpawned >= islandInfo.getMaxMonsters()) {
                event.setCancelled(true);
                return;
            }
        } else if (Animals.class.isAssignableFrom(entityType.getEntityClass())) {
            long currentlySpawned = EntityUtil.getAnimals(creatures).size();
            if (currentlySpawned >= islandInfo.getMaxAnimals()) {
                event.setCancelled(true);
                return;
            }
        } else if (NPC.class.isAssignableFrom(entityType.getEntityClass())) {
            long currentlySpawned = EntityUtil.getNPCs(creatures).size();
            if (currentlySpawned >= islandInfo.getMaxVillagers()) {
                event.setCancelled(true);
                return;
            }
        }
    }

}
