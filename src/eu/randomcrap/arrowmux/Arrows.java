package eu.randomcrap.arrowmux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Arrows implements Listener {
    final private ArrowMux                        plugin;
    final private int                             selectorSize;
    final private HashMap<String, Object>         arrows;
    final private boolean                         fireIgniteBlock;
    final private float                           explosivePower;
    final private boolean                         explosiveBlockDamage;
    final private boolean                         explosiveFire;
    final private String                          commandCommand;
    final private boolean                         commandTriggerOnMob;
    final private boolean                         commandTriggerOnPlayer;
    final private boolean                         commandTriggerOnHit;
    private final long                            iceFreezeDurationWater;
    private final long                            iceFreezeDuration;
    private final boolean                         iceFreezeWater;
    final static private HashMap<Integer, String> mobs = new HashMap<Integer, String>() {
                                                           private static final long serialVersionUID = -1328199051523623897L;
                                                           {
                                                               put(61, "Blaze");
                                                               put(59, "Cave Spider");
                                                               put(50, "Creeper");
                                                               put(58, "Enderman");
                                                               put(67, "Endermite");
                                                               put(56, "Ghast");
                                                               put(68, "Guardian");
                                                               put(62, "Magma Cube");
                                                               put(60, "Silverfish");
                                                               put(51, "Skeleton");
                                                               put(55, "Slime");
                                                               put(52, "Spider");
                                                               put(66, "Witch");
                                                               put(54, "Zombie");
                                                               put(57, "Pig Zombie");
                                                               put(65, "Bat");
                                                               put(93, "Chicken");
                                                               put(92, "Cow");
                                                               put(100, "Horse");
                                                               put(96, "Mushroom Cow");
                                                               put(98, "Ocelot");
                                                               put(90, "Pig");
                                                               put(91, "Sheep");
                                                               put(94, "Squid");
                                                               put(95, "Wolf");
                                                               put(101, "Rabbit");
                                                               put(120, "Villager");
                                                           }
                                                       };

    public Arrows(final ArrowMux arrowMux) {
        this.plugin = arrowMux;
        this.arrows = new HashMap<String, Object>();
        final FileConfiguration config = plugin.getConfig();
        final Map<String, Object> arrows = config.getConfigurationSection("arrows")
            .getValues(false);
        final int arrowAmount = arrows.size() + 1;
        this.selectorSize = arrowAmount % 9 == 0 ? arrowAmount : arrowAmount + 9 - arrowAmount % 9;
        for (final Entry<String, Object> entry : arrows.entrySet()) {
            final String arrowName = entry.getKey().substring(0, 1).toUpperCase()
                                     + entry.getKey().substring(1) + " Arrow";
            this.arrows.put(arrowName, null);
            final MemorySection arrowConfig = (MemorySection) entry.getValue();

            final List<String> craftShapeL = arrowConfig.getStringList("craftShape");
            final String[] craftShape = craftShapeL.toArray(new String[craftShapeL.size()]);
            final Map<String, Object> craftMaterials = arrowConfig
                .getConfigurationSection("craftMaterials").getValues(false);
            final int craftAmount = arrowConfig.getInt("craftAmount");

            final ItemStack arrowStack = new ItemStack(262, craftAmount);
            nameStack(arrowStack, arrowName, arrowConfig.getStringList("description"));
            final ShapedRecipe recipe = new ShapedRecipe(arrowStack).shape(craftShape[0],
                                                                           craftShape[1],
                                                                           craftShape[2]);
            for (final Entry<String, Object> materialEntry : craftMaterials.entrySet()) {
                recipe.setIngredient(materialEntry.getKey().charAt(0),
                                     Material.getMaterial((Integer) materialEntry.getValue()));
            }
            plugin.getServer().addRecipe(recipe);
        }
        this.fireIgniteBlock = config.getBoolean("arrows.fire.igniteBlock");
        this.explosiveFire = config.getBoolean("arrows.explosive.fire");
        this.explosivePower = (float) config.getDouble("arrows.explosive.power");
        this.explosiveBlockDamage = config.getBoolean("arrows.explosive.blockDamage");
        this.commandCommand = config.getString("arrows.command.command");
        this.commandTriggerOnMob = config.getBoolean("arrows.command.TriggerOnMob");
        this.commandTriggerOnPlayer = config.getBoolean("arrows.command.triggerOnPlayer");
        this.commandTriggerOnHit = config.getBoolean("arrows.command.triggerOnHit");
        this.iceFreezeDurationWater = config.getLong("arrows.ice.freezeDurationWater");
        this.iceFreezeDuration = config.getLong("arrows.ice.freezeDuration");
        this.iceFreezeWater = config.getBoolean("arrows.ice.freezeWater");
    }

    // we don't care about cancelled because selecting should be possible
    // everywhere
    // and selecting cancels other events already
    // shooting is an extra event
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerClick(final PlayerInteractEvent event) {
        final Action action = event.getAction();
        final ItemStack itemInHand = event.getItem();
        if (itemInHand == null)
            return;
        if (itemInHand.getType() == Material.BOW) {
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                openArrowSelector(event, itemInHand);
            }
            else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                // TODO: intercept normal arrows consuming better arrows
                final Player player = event.getPlayer();
                if (!player.hasPermission("arrowmux.shoot"))
                    return;
                final PlayerInventory inv = player.getInventory();
                final int index = inv.first(Material.ARROW);
                player.setMetadata("shotArrow", new FixedMetadataValue(plugin, new Object[] {
                        index, inv.getItem(index).clone(), 20 }));
            }
        }
    }

    private void openArrowSelector(final PlayerInteractEvent event, final ItemStack itemInHand) {
        // TODO: intercept allowing inv normal arrows selection or allow all
        // special arrow selection in inventory too
        // TODO: mobs still spawning after mob egg was thrown away, check for
        // mob egg in inventory
        // TODO: pvp off, freezing etc still wokring?
        final Player player = event.getPlayer();
        if (!player.isSneaking() || !player.hasPermission("arrowmux.select"))
            return;
        event.setCancelled(true);
        final ItemMeta bowMeta = itemInHand.getItemMeta();
        String currentArrow = "Arrow";
        if (bowMeta != null && bowMeta.hasLore()) {
            final String lore = bowMeta.getLore().get(0);
            if (arrows.containsKey(lore)) {
                currentArrow = lore;
            }
        }
        final Inventory arrowSelector = plugin.getServer().createInventory(null,
                                                                           selectorSize,
                                                                           "Arrow Selector: "
                                                                               + currentArrow);
        // arrowSelector.setMaxStackSize(4 * 9 * 64);
        arrowSelector.setMaxStackSize(999);
        for (final ItemStack stack : player.getInventory()) {
            if (stack != null && stack.getType() == Material.ARROW) {
                ItemMeta meta = null;
                if (stack.hasItemMeta() && (meta = stack.getItemMeta()).hasDisplayName()
                    && meta.hasLore()) {
                    final String name = meta.getDisplayName();
                    if (arrows.containsKey(name)) {
                        arrowSelector.addItem(stack);
                    }
                }
                else if (!stack.hasItemMeta()) {
                    arrowSelector.addItem(stack);
                }
            }
        }
        player.openInventory(arrowSelector);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onArrowShoot(final EntityShootBowEvent event) {
        final ItemStack bow = event.getBow();
        if (bow != null && event.getProjectile() instanceof Arrow) {
            final Player player = (Player) event.getEntity();
            if (!player.hasPermission("arrowmux.shoot"))
                return;
            if (bow.hasItemMeta()) {
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        final ItemMeta meta = bow.getItemMeta();
                        final Inventory inv = player.getInventory();
                        final Object[] test = (Object[]) getMetaData(player, "shotArrow");
                        // set new (consumed) itemstack to stack before shoot
                        inv.setItem((Integer) test[0], (ItemStack) test[1]);
                        if (meta.hasLore()) {
                            String equippedArrow = meta.getLore().get(0);
                            if (equippedArrow != null && arrows.containsKey(equippedArrow)) {
                                for (final ItemStack stack : inv) {
                                    ItemMeta stackMeta = null;
                                    if (stack != null && stack.getType() == Material.ARROW
                                        && stack.hasItemMeta()
                                        && (stackMeta = stack.getItemMeta()).hasDisplayName()
                                        && equippedArrow.equals(stackMeta.getDisplayName())) {
                                        final int arrowAmount = stack.getAmount();
                                        if (player.getGameMode() != GameMode.CREATIVE
                                            && !bow.getEnchantments()
                                                .containsKey(Enchantment.ARROW_INFINITE)) {
                                            if (arrowAmount > 1) {
                                                stack.setAmount(arrowAmount - 1);
                                            }
                                            else {
                                                inv.remove(stack);
                                            }
                                        }
                                        final Arrow arrow = (Arrow) event.getProjectile();
                                        if (equippedArrow.equals("Grapple Arrow")) {
                                            final Vector arrowVelocity = arrow.getVelocity()
                                                .clone();
                                            plugin.getServer().getScheduler()
                                                .scheduleSyncDelayedTask(plugin, new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        player.setVelocity(arrowVelocity
                                                            .multiply(3));
                                                    }
                                                }, 20);
                                        }
                                        else if (equippedArrow.equals("Fire Arrow")) {
                                            arrow.setFireTicks(200);
                                        }
                                        else if (equippedArrow.equals("Shower Arrow")) {
                                            final Vector vec = arrow.getVelocity();
                                            player.launchProjectile(Arrow.class)
                                                .setVelocity(changeDirection(vec, 0, 0, 0));
                                            /*
                                             * doesn't work
                                             *
                                             * player.launchProjectile(Arrow.class
                                             * )
                                             * .setVelocity(changeDirection(vec,
                                             * -5, 0, 0));
                                             * player.launchProjectile
                                             * (Arrow.class)
                                             * .setVelocity(changeDirection(vec,
                                             * 0, 0, 5));
                                             * player.launchProjectile
                                             * (Arrow.class)
                                             * .setVelocity(changeDirection(vec,
                                             * 0, 0, -5));
                                             */
                                        }

                                        else if (equippedArrow.equals("Ice Arrow")
                                                 && iceFreezeWater) {
                                            schedIce(arrow, plugin.getServer().getScheduler());
                                        }
                                        else if (equippedArrow.equals("Mob Arrow")) {
                                            equippedArrow = meta.getLore().get(1) + " "
                                                            + equippedArrow;
                                        }
                                        arrow.setMetadata("arrowType",
                                                          new FixedMetadataValue(plugin,
                                                                                 equippedArrow));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }, 1);
            }
        }
    }

    private void schedIce(final Arrow arrow, final BukkitScheduler sched) {
        sched.scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                Material mat = null;
                final Block block = arrow.getLocation().getBlock();
                if ((mat = block.getType()) == Material.WATER || mat == Material.STATIONARY_WATER) {
                    arrow.remove();

                    // check if surrounding blocks (which should become ice) are water
                    final List<Block> icyBlocks = new ArrayList<Block>();
                    icyBlocks.add(block);
                    for (final Block blk : new Block[] { block, block.getRelative(BlockFace.EAST),
                            block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.SOUTH),
                            block.getRelative(BlockFace.WEST), block.getRelative(BlockFace.DOWN) }) {
                        final Material mt = blk.getType();
                        if (mt == Material.WATER || mt == Material.STATIONARY_WATER) {
                            icyBlocks.add(blk);
                            blk.setType(Material.ICE);
                        }
                    }
                    block.setType(Material.ICE);

                    if (iceFreezeDurationWater > 0) {
                        sched.scheduleSyncDelayedTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                for (final Block bk : icyBlocks) {
                                    // check blocks again, ice blocks may have become something else in the meantime
                                    if (bk.getType() == Material.ICE) {
                                        bk.setType(Material.WATER);
                                    }
                                }
                                block.getWorld().playSound(block.getLocation(), Sound.WATER, 1, 1);
                            }
                        }, 20 * iceFreezeDurationWater);
                    }
                }
                else if (arrow.isValid()) {
                    schedIce(arrow, sched);
                }
            }
        }, 2);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSelectArrow(final InventoryClickEvent event) {
        Inventory inv = null;
        String title = null;
        if ((title = (inv = event.getInventory()).getTitle()).matches("^(Arrow|Mob) Selector.*$")) {
            event.setCancelled(true);
            final Player player = (Player) event.getWhoClicked();
            final ItemStack clickedItem = event.getCurrentItem();
            if (inv.contains(clickedItem)) {
                ItemMeta meta = null;
                final ItemStack itemInHand = player.getItemInHand();
                if (itemInHand != null && itemInHand.getType() == Material.BOW) {
                    final ItemMeta itemInHandMeta = itemInHand.getItemMeta();
                    if (title.matches("^Arrow Selector:.*$")) {
                        if (clickedItem.hasItemMeta()
                            && (meta = clickedItem.getItemMeta()).hasDisplayName()) {
                            final String arrowType = meta.getDisplayName();
                            if (arrowType.equals("Mob Arrow")) {
                                final Inventory mobSelector = plugin.getServer()
                                    .createInventory(null, 27, "Mob Selector");
                                mobSelector.setMaxStackSize(999);
                                for (final ItemStack invStack : player.getInventory()) {
                                    if (invStack != null
                                        && invStack.getType() == Material.MONSTER_EGG
                                        && (invStack.getData().getData() & 0xFF) >= 50) {
                                        mobSelector.addItem(invStack);
                                    }
                                }
                                event.getView().close();
                                plugin.getServer().getScheduler()
                                    .scheduleSyncDelayedTask(plugin, new Runnable() {
                                        @Override
                                        public void run() {
                                            player.openInventory(mobSelector);
                                        }
                                    }, 1);
                                return;
                            }
                            else {
                                itemInHandMeta.setLore(Arrays.asList(arrowType));
                            }
                        }
                        else {
                            itemInHandMeta.setLore(null);
                        }
                    }
                    else {
                        itemInHandMeta.setLore(Arrays.asList("Mob Arrow", mobs.get(clickedItem
                            .getData().getData() & 0xFF)));
                    }
                    itemInHand.setItemMeta(itemInHandMeta);
                    event.getView().close();
                }
            }
        }
    }

    // not sure if this works with all kind of protection
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowHit(final ProjectileHitEvent event) {
        if (event.getEntityType() == EntityType.ARROW) {
            final Arrow arrow = (Arrow) event.getEntity();
            if (arrow != null && arrow.hasMetadata("arrowType")) {
                final String arrowType = (String) getMetaData(arrow, "arrowType");
                final Player player = (Player) arrow.getShooter();
                final World world = arrow.getWorld();
                final Location loc = arrow.getLocation();
                if (arrowType.equals("Explosive Arrow")) {
                    world.createExplosion(loc.getX(), loc.getY(), loc.getZ(), explosivePower,
                                          explosiveFire, explosiveBlockDamage);
                    arrow.remove();
                }
                else if (arrowType.equals("Teleport Arrow")) {
                    plugin.getServer().getScheduler()
                        .scheduleSyncDelayedTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                player.teleport(arrow);
                                arrow.remove();
                                player.playSound(loc, Sound.ENDERMAN_TELEPORT, 1, 1);
                            }
                        }, 1);
                }
                else if (arrowType.equals("Fire Arrow") && fireIgniteBlock
                         && arrow.getFireTicks() > 0) {
                    arrow.getLocation().getBlock().setType(Material.FIRE);
                }
                else if (arrowType.equals("Lightning Arrow")) {
                    world.strikeLightning(loc);
                    arrow.remove();
                }
                else if (arrowType.equals("Command Arrow") && commandTriggerOnHit
                         && commandCommand != null && commandCommand != "") {
                    String command = this.commandCommand;
                    final Server server = plugin.getServer();
                    command = command.replaceAll("&SHOOTER&", player.getName());
                    command = command.replaceAll("&WORLD&", world.getName());
                    command = command.replaceAll("&X&",
                                                 ((Integer) ((Double) loc.getX()).intValue())
                                                     .toString());
                    command = command.replaceAll("&Y&",
                                                 ((Integer) ((Double) loc.getY()).intValue())
                                                     .toString());
                    command = command.replaceAll("&Z&",
                                                 ((Integer) ((Double) loc.getZ()).intValue())
                                                     .toString());
                    server.dispatchCommand(player, command);
                    arrow.remove();
                }
                else if (arrowType.equals("Ice Arrow")) {
                    arrow.remove();
                }
                else if (arrowType.matches("^.* Mob Arrow$")) {
                    String mob = null;
                    final Matcher m = Pattern.compile("^(.*) Mob Arrow$").matcher(arrowType);
                    if (m.matches()) {
                        mob = m.group(1);
                    }
                    if (mob != null) {
                        final EntityType mobType = EntityType.valueOf(mob.replace(' ', '_')
                            .toUpperCase());
                        if (mobType != null) {
                            world.spawnEntity(loc, mobType);
                            arrow.remove();
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityHit(final EntityDamageByEntityEvent event) {
        final Entity damager = event.getDamager();
        final Entity damagedEntity = event.getEntity();
        // prevent player from hitting stuff when player wants to shoot actually
        if (damager instanceof Player) {
            final Player player = (Player) damager;
            final ItemStack itemInHand = player.getItemInHand();
            if (itemInHand != null && itemInHand.getType() == Material.BOW && player.isSneaking()) {
                event.setCancelled(true);
            }
            plugin
                .getServer()
                .getPluginManager()
                .callEvent(new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, itemInHand, null,
                                                   null));
        }
        else if (damager instanceof Arrow) {
            final Arrow arrow = (Arrow) damager;
            if (arrow.getShooter() instanceof Player && arrow.hasMetadata("arrowType")) {
                final Player shooterPlayer = (Player) arrow.getShooter();
                final String arrowType = (String) getMetaData(arrow, "arrowType");
                if (damagedEntity instanceof LivingEntity) {
                    if (arrowType.equals("Teleport Arrow")) {
                        arrow.setBounce(false);
                        damagedEntity.teleport(shooterPlayer);
                    }
                    else if (arrowType.equals("Command Arrow") && commandCommand != null
                             && commandCommand != "") {
                        String command = this.commandCommand;
                        if (damagedEntity instanceof Player) {
                            if (!commandTriggerOnPlayer)
                                return;
                            command = command.replaceAll("&TARGET&",
                                                         ((Player) damagedEntity).getName());
                        }
                        else {
                            if (!commandTriggerOnMob)
                                return;
                            command = command.replaceAll("&TARGET&", ((LivingEntity) damagedEntity)
                                .getType().getName());
                        }
                        command = command.replaceAll("&SHOOTER&", shooterPlayer.getName());
                        command = command.replaceAll("&WORLD&", arrow.getWorld().getName());
                        final Location loc = arrow.getLocation();
                        command = command.replaceAll("&X&", ((Integer) ((Double) loc.getX())
                            .intValue()).toString());
                        command = command.replaceAll("&Y&", ((Integer) ((Double) loc.getY())
                            .intValue()).toString());
                        command = command.replaceAll("&Z&", ((Integer) ((Double) loc.getZ())
                            .intValue()).toString());
                        plugin.getServer().dispatchCommand(shooterPlayer, command);
                    }
                    else if (arrowType.equals("Ice Arrow")) {
                        final Block block = damagedEntity.getLocation().getBlock();
                        damagedEntity.setVelocity(new Vector());
                        final BukkitScheduler sched = plugin.getServer().getScheduler();
                        sched.scheduleSyncDelayedTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                damagedEntity.teleport(block.getLocation().add(0.5, 0, 0.5));
                            }
                        }, 1);

                        // check if surrounding blocks (which should become ice) are air or water
                        final List<Block> icyBlocks = new ArrayList<Block>();
                        for (final Block blk : new Block[] { block, block.getRelative(0, 1, 0),
                                block.getRelative(0, -1, 0), block.getRelative(1, 0, 0),
                                block.getRelative(-1, 0, 0), block.getRelative(0, 0, 1),
                                block.getRelative(0, 0, -1), block.getRelative(1, 1, 0),
                                block.getRelative(-1, 1, 0), block.getRelative(0, 1, 1),
                                block.getRelative(0, 1, -1), block.getRelative(0, 2, 0), }) {
                            final Material blockType = blk.getType();
                            icyBlocks.add(blk);
                            if (blockType == Material.AIR || blockType == Material.WATER
                                || blockType == Material.STATIONARY_WATER) {
                                blk.setType(Material.ICE);
                            }
                        }
                        sched.scheduleSyncDelayedTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                for (final Block blk : icyBlocks) {
                                    // check blocks again, ice blocks may have become something else in the meantime
                                    // remove water blocks to prevent flood
                                    final Material blockType = blk.getType();
                                    if (blockType == Material.ICE || blockType == Material.WATER
                                        || blockType == Material.STATIONARY_WATER) {
                                        blk.setType(Material.AIR);
                                    }
                                }
                            }
                        }, iceFreezeDuration * 20);
                    }
                }
            }
        }
    }

    final private Object getMetaData(final Object obj, final String key) {
        List<MetadataValue> meta = null;
        if (obj instanceof Entity) {
            meta = ((Entity) obj).getMetadata(key);
        }
        else if (obj instanceof Block) {
            meta = ((Block) obj).getMetadata(key);
        }
        else if (obj instanceof Player) {
            meta = ((Player) obj).getMetadata(key);
        }
        final String pluginName = plugin.getDescription().getName();
        for (final MetadataValue value : meta) {
            if (value.getOwningPlugin().getDescription().getName().equals(pluginName))
                return value.value();
        }
        return null;
    }

    final void nameStack(final ItemStack stack, final String name, final List<String> desc) {
        final ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if (desc != null) {
            meta.setLore(desc);
        }
        stack.setItemMeta(meta);
    }

    final private Vector changeDirection(final Vector v, final double radius, final double yaw,
                                         final double pitch) {
        final double[] coords = sphericalToCartesian(v.getX(), v.getY(), v.getZ());
        coords[0] += radius;
        coords[1] += yaw;
        coords[2] += pitch;
        final double[] newcoords = sphericalToCartesian(coords[0], coords[1], coords[2]);
        return new Vector(newcoords[0], newcoords[1], newcoords[1]);
    }

    public static double[] cartesianToSpherical(final double x, final double y, final double z) {
        final double rthetaphi[] = new double[3];
        rthetaphi[0] = Math.sqrt(x * x + y * y + z * z);
        rthetaphi[1] = Math.acos(z / rthetaphi[0]);
        rthetaphi[2] = Math.atan2(y, x);
        return rthetaphi;
    }

    public static double[] sphericalToCartesian(final double r, final double theta, final double phi) {
        final double xyz[] = new double[3];
        xyz[0] = r * Math.sin(theta) * Math.cos(phi);
        xyz[1] = r * Math.sin(theta) * Math.sin(phi);
        xyz[2] = r * Math.cos(theta);
        return xyz;
    }

}
