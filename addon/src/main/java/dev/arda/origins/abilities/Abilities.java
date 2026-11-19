package dev.arda.origins.abilities;

import com.starshootercity.abilities.types.Ability;
import com.starshootercity.abilities.types.VisibleAbility;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * All custom Tolkien abilities for the "arda" Origins-Reborn addon.
 * Every ability is a VisibleAbility, so it shows in the selection GUI.
 * Keys are arda:<id>, which is what the origin JSON files reference.
 */
public final class Abilities {
    private Abilities() {}

    /** Abilities that need a once-a-second update implement this. */
    public interface Tickable {
        void tick(Player player);
    }

    public static List<Ability> all() {
        return List.of(
                // Ancestries
                new GiftOfMen(), new LastStand(), new EldarBody(), new EldarLore(),
                new WoodlandStride(), new FalmariLungs(), new DelversInstinct(), new ForgeHardened(),
                new SecondBreakfast(), new FortunateFolk(), new Bloodlust(), new IronStomach(),
                new Moonbound(),
                // Classes
                new FirstStrike(), new Cleave(), new KeenTracker(), new Wayfarer(),
                new ShieldWall(), new KingsHands(), new WizardStaff(), new GentleFriend(),
                new MasterSmith(), new Backstab(), new QuietSteps(), new SongOfValor(), new SilverTongue()
        );
    }

    // ------------------------------------------------------------------ helpers

    public abstract static class Base implements VisibleAbility {
        private final Key key;
        private final String title;
        private final String description;

        protected Base(String id, String title, String description) {
            this.key = Key.key("arda", id);
            this.title = title;
            this.description = description;
        }

        @Override
        public Key getKey() {
            return key;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public String description() {
            return description;
        }
    }

    /** Simple per-player cooldown in milliseconds. */
    static final class Cooldown {
        private final Map<UUID, Long> until = new HashMap<>();

        boolean tryUse(Player p, long ms) {
            long now = System.currentTimeMillis();
            Long end = until.get(p.getUniqueId());
            if (end != null && end > now) {
                long left = (end - now + 999) / 1000;
                p.sendActionBar(Component.text("Ready in " + left + "s", NamedTextColor.GRAY));
                return false;
            }
            until.put(p.getUniqueId(), now + ms);
            return true;
        }

        /** Same as tryUse but silent (for passive triggers). */
        boolean tryUseQuiet(Player p, long ms) {
            long now = System.currentTimeMillis();
            Long end = until.get(p.getUniqueId());
            if (end != null && end > now) return false;
            until.put(p.getUniqueId(), now + ms);
            return true;
        }
    }

    static void effect(LivingEntity target, PotionEffectType type, int ticks, int amplifier) {
        target.addPotionEffect(new PotionEffect(type, ticks, amplifier, true, false, true));
    }

    static boolean holdsShield(Player p) {
        return p.getInventory().getItemInMainHand().getType() == Material.SHIELD
                || p.getInventory().getItemInOffHand().getType() == Material.SHIELD;
    }

    static boolean isMelee(EntityDamageByEntityEvent e) {
        return e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
    }

    static boolean isRightClick(Action a) {
        return a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
    }

    // ================================================================ ANCESTRIES

    /** Edain: the Gift of Men. */
    public static final class GiftOfMen extends Base implements Listener {
        public GiftOfMen() {
            super("gift_of_men", "Gift of Men",
                    "Men are quick to learn and adapt. You gain 25% more experience from all sources.");
        }

        @EventHandler
        public void onXp(PlayerExpChangeEvent e) {
            runForAbility(e.getPlayer(), p -> {
                int amount = e.getAmount();
                if (amount > 0) e.setAmount((int) Math.ceil(amount * 1.25));
            });
        }
    }

    /** Edain / Peredhil: hope in the darkest hour. */
    public static final class LastStand extends Base implements Listener {
        private final Cooldown cooldown = new Cooldown();

        public LastStand() {
            super("last_stand", "Hope Unquenched",
                    "When a blow would leave you below 30% health, you gain Resistance and Regeneration for 8 seconds. Once every 90 seconds.");
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onDamage(EntityDamageEvent e) {
            if (!(e.getEntity() instanceof Player victim)) return;
            runForAbility(victim, p -> {
                var attr = p.getAttribute(Attribute.MAX_HEALTH);
                double max = attr == null ? 20.0 : attr.getValue();
                double after = p.getHealth() - e.getFinalDamage();
                if (after <= 0 || after > max * 0.3) return;
                if (!cooldown.tryUseQuiet(p, 90_000)) return;
                effect(p, PotionEffectType.RESISTANCE, 160, 0);
                effect(p, PotionEffectType.REGENERATION, 160, 0);
                p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);
                p.sendActionBar(Component.text("Hope unquenched!", NamedTextColor.GOLD));
            });
        }
    }

    /** Elves: no sickness. */
    public static final class EldarBody extends Base implements Listener {
        public EldarBody() {
            super("eldar_body", "Undying Body",
                    "The Eldar do not sicken as Men do. You are immune to Poison and Hunger effects.");
        }

        @EventHandler
        public void onEffect(EntityPotionEffectEvent e) {
            if (!(e.getEntity() instanceof Player player)) return;
            PotionEffect added = e.getNewEffect();
            if (added == null) return;
            PotionEffectType type = added.getType();
            if (type != PotionEffectType.POISON && type != PotionEffectType.HUNGER) return;
            runForAbility(player, p -> e.setCancelled(true));
        }
    }

    /** Noldor: lore and craft. */
    public static final class EldarLore extends Base implements Listener {
        public EldarLore() {
            super("eldar_lore", "Deep Lore",
                    "The Noldor are masters of craft and lore. Enchanting costs you one fewer experience level.");
        }

        @EventHandler
        public void onEnchant(EnchantItemEvent e) {
            runForAbility(e.getEnchanter(), p -> {
                int minimum = e.whichButton() + 1;
                e.setExpLevelCost(Math.max(minimum, e.getExpLevelCost() - 1));
            });
        }
    }

    /** Sindar: at home beneath the trees. */
    public static final class WoodlandStride extends Base implements Tickable {
        public WoodlandStride() {
            super("woodland_stride", "Woodland Stride",
                    "You move with the speed of the wood. Gain Speed while in forests, taigas, jungles and groves.");
        }

        @Override
        public void tick(Player player) {
            runForAbility(player, p -> {
                String biome = p.getLocation().getBlock().getBiome().getKey().getKey();
                if (biome.contains("forest") || biome.contains("taiga") || biome.contains("jungle")
                        || biome.contains("grove") || biome.contains("woods")) {
                    effect(p, PotionEffectType.SPEED, 60, 0);
                }
            });
        }
    }

    /** Teleri: breath of the sea. */
    public static final class FalmariLungs extends Base implements Tickable {
        public FalmariLungs() {
            super("falmari_lungs", "Breath of the Sea",
                    "The Teleri are born of the shore. Your breath lasts about three times as long underwater.");
        }

        @Override
        public void tick(Player player) {
            runForAbility(player, p -> {
                if (p.getEyeLocation().getBlock().getType() != Material.WATER) return;
                p.setRemainingAir(Math.min(p.getMaximumAir(), p.getRemainingAir() + 13));
            });
        }
    }

    /** Dwarves: at home in the deep places. */
    public static final class DelversInstinct extends Base implements Tickable {
        public DelversInstinct() {
            super("delvers_instinct", "Delver's Instinct",
                    "In deep, unlit tunnels (below sea level, out of the sky's light) you gain Haste and can see by the touch of stone.");
        }

        @Override
        public void tick(Player player) {
            runForAbility(player, p -> {
                if (p.getLocation().getBlockY() >= 50) return;
                if (p.getLocation().getBlock().getLightFromSky() >= 4) return;
                effect(p, PotionEffectType.HASTE, 60, 0);
                effect(p, PotionEffectType.NIGHT_VISION, 320, 0);
            });
        }
    }

    /** Dwarves and smiths: hardened against fire. Shared by Naugrim and Inventor. */
    public static final class ForgeHardened extends Base implements Listener {
        private static final Set<EntityDamageEvent.DamageCause> FIRE_CAUSES = EnumSet.of(
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.LAVA,
                EntityDamageEvent.DamageCause.HOT_FLOOR);

        public ForgeHardened() {
            super("forge_hardened", "Forge-hardened",
                    "Long years at the forge have toughened you. You take 40% less damage from fire, lava and hot floors.");
        }

        @EventHandler
        public void onDamage(EntityDamageEvent e) {
            if (!(e.getEntity() instanceof Player victim)) return;
            if (!FIRE_CAUSES.contains(e.getCause())) return;
            runForAbility(victim, p -> e.setDamage(e.getDamage() * 0.6));
        }
    }

    /** Hobbits: food is a joy. */
    public static final class SecondBreakfast extends Base implements Listener {
        public SecondBreakfast() {
            super("second_breakfast", "Second Breakfast",
                    "Hobbits know how to enjoy a meal. Eating food restores 1 extra hunger point and a little extra saturation.");
        }

        @EventHandler
        public void onEat(PlayerItemConsumeEvent e) {
            if (!e.getItem().getType().isEdible()) return;
            runForAbility(e.getPlayer(), p -> {
                p.setFoodLevel(Math.min(20, p.getFoodLevel() + 1));
                p.setSaturation(Math.min(p.getFoodLevel(), p.getSaturation() + 2f));
            });
        }
    }

    /** Hobbits: good fortune. */
    public static final class FortunateFolk extends Base implements Tickable {
        public FortunateFolk() {
            super("fortunate_folk", "Fortunate Folk",
                    "Luck follows Hobbits about. You have a permanent Luck effect, improving fishing and loot.");
        }

        @Override
        public void tick(Player player) {
            runForAbility(player, p -> effect(p, PotionEffectType.LUCK, 100, 0));
        }
    }

    /** Uruk-hai: fury of battle. */
    public static final class Bloodlust extends Base implements Listener {
        public Bloodlust() {
            super("bloodlust", "Battle-fury",
                    "Each kill fills you with fury: Speed for 8 seconds and Strength for 5 seconds.");
        }

        @EventHandler
        public void onKill(EntityDeathEvent e) {
            Player killer = e.getEntity().getKiller();
            if (killer == null) return;
            runForAbility(killer, p -> {
                effect(p, PotionEffectType.SPEED, 160, 0);
                effect(p, PotionEffectType.STRENGTH, 100, 0);
            });
        }
    }

    /** Uruk-hai: nothing spoils. */
    public static final class IronStomach extends Base implements Listener {
        public IronStomach() {
            super("iron_stomach", "Iron Stomach",
                    "Spoiled and foul food cannot harm you. Rotten flesh, spider eyes, poisonous potatoes and the like give no bad effects.");
        }

        @EventHandler
        public void onEffect(EntityPotionEffectEvent e) {
            if (!(e.getEntity() instanceof Player player)) return;
            if (e.getCause() != EntityPotionEffectEvent.Cause.FOOD) return;
            PotionEffect added = e.getNewEffect();
            if (added == null) return;
            PotionEffectType type = added.getType();
            if (type == PotionEffectType.HUNGER || type == PotionEffectType.POISON || type == PotionEffectType.NAUSEA) {
                runForAbility(player, p -> e.setCancelled(true));
            }
        }
    }

    /** Beorning: strongest under the moon. */
    public static final class Moonbound extends Base implements Tickable {
        public Moonbound() {
            super("moonbound", "Moonbound",
                    "By night the skin-changer's strength rises: Strength I. Under a gibbous moon you also gain Speed, and under a full moon you gain Strength II, Speed and Resistance.");
        }

        @Override
        public void tick(Player player) {
            runForAbility(player, p -> {
                World world = p.getWorld();
                if (world.getEnvironment() != World.Environment.NORMAL) return;
                long time = world.getTime();
                if (time < 13000 || time > 23000) return; // night only
                // 0 = full moon, 1 and 7 = gibbous, 4 = new moon
                int phase = (int) ((world.getFullTime() / 24000L) % 8L);
                switch (phase) {
                    case 0 -> {
                        effect(p, PotionEffectType.STRENGTH, 60, 1);
                        effect(p, PotionEffectType.SPEED, 60, 0);
                        effect(p, PotionEffectType.RESISTANCE, 60, 0);
                    }
                    case 1, 7 -> {
                        effect(p, PotionEffectType.STRENGTH, 60, 0);
                        effect(p, PotionEffectType.SPEED, 60, 0);
                    }
                    default -> effect(p, PotionEffectType.STRENGTH, 60, 0);
                }
            });
        }
    }

    // ================================================================== CLASSES

    /** Fighter. */
    public static final class FirstStrike extends Base implements Listener {
        private final Map<UUID, Long> lastAttack = new HashMap<>();

        public FirstStrike() {
            super("first_strike", "First Strike",
                    "Your opening blow after 5 seconds without attacking deals 20% more damage.");
        }

        @EventHandler
        public void onHit(EntityDamageByEntityEvent e) {
            if (!(e.getDamager() instanceof Player damager) || !isMelee(e)) return;
            runForAbility(damager, p -> {
                long now = System.currentTimeMillis();
                Long last = lastAttack.put(p.getUniqueId(), now);
                if (last == null || now - last >= 5000) {
                    e.setDamage(e.getDamage() * 1.2);
                }
            });
        }
    }

    /** Fighter. */
    public static final class Cleave extends Base implements Listener {
        private static boolean cleaving = false;

        public Cleave() {
            super("cleave", "Cleave",
                    "Sword and axe blows carry through: a hostile mob next to your target takes half the damage.");
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onHit(EntityDamageByEntityEvent e) {
            if (cleaving) return;
            if (!(e.getDamager() instanceof Player damager) || !isMelee(e)) return;
            if (!(e.getEntity() instanceof LivingEntity victim)) return;
            runForAbility(damager, p -> {
                String weapon = p.getInventory().getItemInMainHand().getType().name();
                if (!weapon.endsWith("_SWORD") && !weapon.endsWith("_AXE")) return;
                double splash = e.getFinalDamage() * 0.5;
                for (Entity near : victim.getNearbyEntities(2.5, 1.5, 2.5)) {
                    if (near instanceof Monster other && other != victim) {
                        cleaving = true;
                        try {
                            other.damage(splash, p);
                        } finally {
                            cleaving = false;
                        }
                        break;
                    }
                }
            });
        }
    }

    /** Ranger. */
    public static final class KeenTracker extends Base implements Listener {
        public KeenTracker() {
            super("keen_tracker", "Keen Tracker",
                    "Creatures struck by your arrows are revealed with Glowing for 5 seconds.");
        }

        @EventHandler
        public void onArrow(EntityDamageByEntityEvent e) {
            if (!(e.getDamager() instanceof Arrow arrow)) return;
            if (!(arrow.getShooter() instanceof Player shooter)) return;
            if (!(e.getEntity() instanceof LivingEntity victim) || victim instanceof Player) return;
            runForAbility(shooter, p -> effect(victim, PotionEffectType.GLOWING, 100, 0));
        }
    }

    /** Ranger. */
    public static final class Wayfarer extends Base implements Listener {
        public Wayfarer() {
            super("wayfarer", "Wayfarer",
                    "Long roads are your home. A quarter of the time, you lose no hunger when you would have.");
        }

        @EventHandler
        public void onFood(FoodLevelChangeEvent e) {
            if (!(e.getEntity() instanceof Player player)) return;
            if (e.getFoodLevel() >= player.getFoodLevel()) return;
            runForAbility(player, p -> {
                if (ThreadLocalRandom.current().nextDouble() < 0.25) e.setCancelled(true);
            });
        }
    }

    /** Champion. */
    public static final class ShieldWall extends Base implements Listener {
        private static final Set<EntityDamageEvent.DamageCause> IGNORED = EnumSet.of(
                EntityDamageEvent.DamageCause.VOID,
                EntityDamageEvent.DamageCause.SUICIDE,
                EntityDamageEvent.DamageCause.KILL,
                EntityDamageEvent.DamageCause.CUSTOM);

        public ShieldWall() {
            super("shield_wall", "Shield Wall",
                    "While you hold a shield you take 15% less damage, and allies within 5 blocks take 10% less.");
        }

        @EventHandler
        public void onDamage(EntityDamageEvent e) {
            if (!(e.getEntity() instanceof Player victim)) return;
            if (IGNORED.contains(e.getCause())) return;
            if (hasAbility(victim) && holdsShield(victim)) {
                e.setDamage(e.getDamage() * 0.85);
                return;
            }
            for (Entity near : victim.getNearbyEntities(5, 3, 5)) {
                if (near instanceof Player champion && hasAbility(champion) && holdsShield(champion)) {
                    e.setDamage(e.getDamage() * 0.9);
                    return;
                }
            }
        }
    }

    /** Cleric / Healer: "the hands of the king are the hands of a healer". */
    public static final class KingsHands extends Base implements Listener {
        private final Cooldown others = new Cooldown();
        private final Cooldown self = new Cooldown();

        public KingsHands() {
            super("kings_hands", "Hands of the Healer",
                    "Sneak and right-click an ally with an empty hand to grant Regeneration II and cure Poison and Wither (15s cooldown). Do the same in the air to heal yourself (45s).");
        }

        private void heal(Player target) {
            target.removePotionEffect(PotionEffectType.POISON);
            target.removePotionEffect(PotionEffectType.WITHER);
            effect(target, PotionEffectType.REGENERATION, 160, 1);
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.8, 0), 6, 0.3, 0.3, 0.3, 0);
        }

        @EventHandler
        public void onTouch(PlayerInteractEntityEvent e) {
            if (e.getHand() != EquipmentSlot.HAND) return;
            if (!(e.getRightClicked() instanceof Player target)) return;
            runForAbility(e.getPlayer(), p -> {
                if (!p.isSneaking() || !p.getInventory().getItemInMainHand().getType().isAir()) return;
                if (!others.tryUse(p, 15_000)) return;
                heal(target);
            });
        }

        @EventHandler
        public void onSelf(PlayerInteractEvent e) {
            if (e.getHand() != EquipmentSlot.HAND) return;
            if (e.getAction() != Action.RIGHT_CLICK_AIR) return;
            runForAbility(e.getPlayer(), p -> {
                if (!p.isSneaking() || !p.getInventory().getItemInMainHand().getType().isAir()) return;
                if (!self.tryUse(p, 45_000)) return;
                heal(p);
            });
        }
    }

    /** Wizard / Loremaster: Gandalf's light. */
    public static final class WizardStaff extends Base implements Listener {
        private static final Set<EntityType> UNDEAD = EnumSet.of(
                EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
                EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.PHANTOM);
        private final Cooldown cooldown = new Cooldown();

        public WizardStaff() {
            super("wizard_staff", "Light of the Staff",
                    "Right-click with a Blaze Rod as your staff to drive back the shadow: hostile creatures within 6 blocks are thrown back and revealed, and the undead are set alight. 20 second cooldown.");
        }

        @EventHandler
        public void onCast(PlayerInteractEvent e) {
            if (e.getHand() != EquipmentSlot.HAND || !isRightClick(e.getAction())) return;
            if (e.getItem() == null || e.getItem().getType() != Material.BLAZE_ROD) return;
            runForAbility(e.getPlayer(), p -> {
                if (!cooldown.tryUse(p, 20_000)) return;
                World world = p.getWorld();
                world.spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 60, 2.5, 0.8, 2.5, 0.05);
                world.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
                for (Entity near : p.getNearbyEntities(6, 3, 6)) {
                    if (!(near instanceof Monster monster)) continue;
                    Vector away = monster.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                    if (away.lengthSquared() > 0) {
                        monster.setVelocity(away.normalize().multiply(1.2).setY(0.35));
                    }
                    effect(monster, PotionEffectType.GLOWING, 100, 0);
                    if (UNDEAD.contains(monster.getType())) monster.setFireTicks(80);
                }
            });
        }
    }

    /** Druid / Beastfriend: Beren, Huan and the Eagles. */
    public static final class GentleFriend extends Base implements Listener {
        private static final Set<EntityType> BEASTS = EnumSet.of(
                EntityType.WOLF, EntityType.BEE, EntityType.POLAR_BEAR, EntityType.LLAMA,
                EntityType.TRADER_LLAMA, EntityType.PANDA, EntityType.GOAT);

        public GentleFriend() {
            super("gentle_friend", "Friend of Beasts",
                    "Wolves, bees, bears, llamas and other wild beasts will not pick you as a target unless you strike first.");
        }

        @EventHandler
        public void onTarget(EntityTargetLivingEntityEvent e) {
            if (!BEASTS.contains(e.getEntityType())) return;
            if (!(e.getTarget() instanceof Player player)) return;
            EntityTargetEvent.TargetReason reason = e.getReason();
            if (reason == EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY
                    || reason == EntityTargetEvent.TargetReason.CUSTOM) return;
            runForAbility(player, p -> e.setCancelled(true));
        }
    }

    /** Inventor / Smith: self-mending craft. */
    public static final class MasterSmith extends Base implements Tickable {
        private static final EquipmentSlot[] SLOTS = {
                EquipmentSlot.HAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        private final Map<UUID, Long> lastRepair = new HashMap<>();

        public MasterSmith() {
            super("master_smith", "Mending Craft",
                    "Your own good work endures. Your held item and worn armor slowly repair themselves (1 durability every 10 seconds).");
        }

        @Override
        public void tick(Player player) {
            runForAbility(player, p -> {
                long now = System.currentTimeMillis();
                Long last = lastRepair.get(p.getUniqueId());
                if (last != null && now - last < 10_000) return;
                lastRepair.put(p.getUniqueId(), now);
                for (EquipmentSlot slot : SLOTS) {
                    ItemStack item = p.getInventory().getItem(slot);
                    if (item == null || item.getType().isAir()) continue;
                    if (item.getItemMeta() instanceof Damageable meta && meta.hasDamage()) {
                        meta.setDamage(meta.getDamage() - 1);
                        item.setItemMeta(meta);
                        p.getInventory().setItem(slot, item);
                    }
                }
            });
        }
    }

    /** Rogue / Burglar. */
    public static final class Backstab extends Base implements Listener {
        public Backstab() {
            super("backstab", "Backstab",
                    "Melee hits from behind your target deal 35% more damage.");
        }

        @EventHandler
        public void onHit(EntityDamageByEntityEvent e) {
            if (!(e.getDamager() instanceof Player damager) || !isMelee(e)) return;
            if (!(e.getEntity() instanceof LivingEntity victim)) return;
            runForAbility(damager, p -> {
                Vector a = p.getLocation().getDirection().setY(0);
                Vector v = victim.getLocation().getDirection().setY(0);
                if (a.lengthSquared() == 0 || v.lengthSquared() == 0) return;
                if (a.normalize().dot(v.normalize()) > 0.5) {
                    e.setDamage(e.getDamage() * 1.35);
                    victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.1);
                }
            });
        }
    }

    /** Rogue / Burglar. */
    public static final class QuietSteps extends Base implements Listener, Tickable {
        public QuietSteps() {
            super("quiet_steps", "Quiet Steps",
                    "You move a little faster while sneaking.");
        }

        @EventHandler
        public void onSneak(PlayerToggleSneakEvent e) {
            if (!e.isSneaking()) return;
            runForAbility(e.getPlayer(), p -> effect(p, PotionEffectType.SPEED, 40, 0));
        }

        @Override
        public void tick(Player player) {
            runForAbility(player, p -> {
                if (p.isSneaking()) effect(p, PotionEffectType.SPEED, 40, 0);
            });
        }
    }

    /** Bard / Minstrel: the Music of the Ainur. */
    public static final class SongOfValor extends Base implements Listener {
        private final Cooldown cooldown = new Cooldown();

        public SongOfValor() {
            super("song_of_valor", "Song of Valor",
                    "Sound a Goat Horn to lift every player within 16 blocks (including you) with Speed for 20 seconds and Regeneration for 8 seconds. 60 second cooldown.");
        }

        @EventHandler
        public void onHorn(PlayerInteractEvent e) {
            if (!isRightClick(e.getAction())) return;
            if (e.getItem() == null || e.getItem().getType() != Material.GOAT_HORN) return;
            runForAbility(e.getPlayer(), p -> {
                if (!cooldown.tryUse(p, 60_000)) return;
                effect(p, PotionEffectType.SPEED, 400, 0);
                effect(p, PotionEffectType.REGENERATION, 160, 0);
                for (Entity near : p.getNearbyEntities(16, 16, 16)) {
                    if (near instanceof Player ally) {
                        effect(ally, PotionEffectType.SPEED, 400, 0);
                        effect(ally, PotionEffectType.REGENERATION, 160, 0);
                    }
                }
            });
        }
    }

    /** Bard / Minstrel. */
    public static final class SilverTongue extends Base implements Tickable {
        public SilverTongue() {
            super("silver_tongue", "Silver Tongue",
                    "Villagers and wandering traders warm to you. Near them you gain Hero of the Village for cheaper trades.");
        }

        @Override
        public void tick(Player player) {
            runForAbility(player, p -> {
                for (Entity near : p.getNearbyEntities(8, 4, 8)) {
                    if (near instanceof Villager || near instanceof WanderingTrader) {
                        effect(p, PotionEffectType.HERO_OF_THE_VILLAGE, 60, 0);
                        return;
                    }
                }
            });
        }
    }
}
