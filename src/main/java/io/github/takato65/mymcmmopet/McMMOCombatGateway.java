package io.github.takato65.mymcmmopet;

import com.gmail.nossr50.config.WorldBlacklist;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.CombatUtils;
import com.gmail.nossr50.worldguard.WorldGuardManager;
import com.gmail.nossr50.worldguard.WorldGuardUtils;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** All calls to mcMMO internals are kept here so updates have one integration boundary. */
public class McMMOCombatGateway {
    public boolean isWorldBlacklisted(World world) {
        return WorldBlacklist.isWorldBlacklisted(world);
    }

    public boolean isEligible(Player owner, LivingEntity target, double damage) {
        if (CombatUtils.hasIgnoreDamageMetadata(target) || CombatUtils.isInvincible(target, damage)) {
            return false;
        }
        if (ExperienceConfig.getInstance().isNPCInteractionPrevented()
                && (Misc.isNPCEntityExcludingVillagers(owner) || Misc.isNPCEntityExcludingVillagers(target))) {
            return false;
        }
        if (WorldGuardUtils.isWorldGuardLoaded()
                && (!WorldGuardManager.getInstance().hasMainFlag(owner)
                    || !WorldGuardManager.getInstance().hasXPFlag(owner)
                    || !WorldGuardManager.getInstance().hasMainFlag(owner, target.getLocation()))) {
            return false;
        }
        return mcMMO.p.getSkillTools().doesPlayerHaveSkillPermission(owner, PrimarySkillType.TAMING)
                && mcMMO.p.getSkillTools().canCombatSkillsTrigger(PrimarySkillType.TAMING, target);
    }

    public void award(Player owner, LivingEntity target, double multiplier) {
        McMMOPlayer profile = UserManager.getPlayer(owner);
        if (profile != null) {
            // Preserve mcMMO's spawn-source, party and PvP rules, health-loss
            // calculation, combat HP ceiling, and normal XP/level-up pipeline.
            CombatUtils.processCombatXP(profile, target, PrimarySkillType.TAMING, multiplier);
        }
    }
}
