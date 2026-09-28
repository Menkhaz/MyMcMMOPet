package io.github.takato65.mymcmmopet;

import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.Set;

public class CombatExperienceService {
    private final double multiplier;
    private final boolean allowPvp;
    private final Set<String> disabledWorlds;
    private final McMMOCombatGateway gateway;

    public CombatExperienceService(double multiplier, boolean allowPvp, Set<String> disabledWorlds) {
        this(multiplier, allowPvp, disabledWorlds, new McMMOCombatGateway());
    }

    CombatExperienceService(double multiplier, boolean allowPvp, Set<String> disabledWorlds,
                            McMMOCombatGateway gateway) {
        if (!Double.isFinite(multiplier) || multiplier < 0) {
            throw new IllegalArgumentException("XP multiplier must be finite and non-negative");
        }
        this.multiplier = multiplier;
        this.allowPvp = allowPvp;
        this.disabledWorlds = Set.copyOf(disabledWorlds);
        this.gateway = gateway;
    }

    public void award(Player owner, LivingEntity target, double damage) {
        if (multiplier == 0 || owner == null || !owner.isOnline() || !owner.isValid()
                || !target.isValid() || target.isDead() || target instanceof ArmorStand
                || target.getType().name().equals("MANNEQUIN")
                || !Double.isFinite(damage) || damage <= 0
                || owner.getGameMode() == GameMode.CREATIVE || owner.getGameMode() == GameMode.SPECTATOR
                || !owner.hasPermission("mymcmmopet.xp")
                || (!allowPvp && target instanceof Player)
                || disabledWorlds.contains(target.getWorld().getName())
                || gateway.isWorldBlacklisted(target.getWorld())
                || gateway.isWorldBlacklisted(owner.getWorld())
                || !gateway.isEligible(owner, target, damage)) {
            return;
        }
        gateway.award(owner, target, multiplier);
    }
}
