package io.github.takato65.mymcmmopet;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CombatExperienceServiceTest {
    private final Player owner = mock(Player.class);
    private final LivingEntity target = mock(LivingEntity.class);
    private final World world = mock(World.class);
    private final McMMOCombatGateway gateway = mock(McMMOCombatGateway.class);
    private final CombatExperienceService service = service(3, false, Set.of());

    private CombatExperienceService service(double multiplier, boolean pvp, Set<String> disabled) {
        return new CombatExperienceService(multiplier, pvp, disabled, gateway);
    }

    @BeforeEach
    void setUp() {
        when(owner.isOnline()).thenReturn(true);
        when(owner.isValid()).thenReturn(true);
        when(owner.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(owner.hasPermission("mymcmmopet.xp")).thenReturn(true);
        when(owner.getWorld()).thenReturn(world);
        when(target.isValid()).thenReturn(true);
        when(target.getType()).thenReturn(EntityType.ZOMBIE);
        when(target.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(gateway.isEligible(owner, target, 7.5)).thenReturn(true);
    }

    private void noAward() {
        verify(gateway, never()).award(any(), any(), anyDouble());
    }

    @Test
    void delegatesEligibleDamageWithConfiguredMultiplier() {
        service.award(owner, target, 7.5);
        verify(gateway).award(owner, target, 3);
        service(0.5, false, Set.of()).award(owner, target, 7.5);
        verify(gateway).award(owner, target, 0.5);
    }

    @Test
    void rejectsZeroNegativeAndNonFiniteDamage() {
        for (double damage : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            service.award(owner, target, damage);
        }
        noAward();
    }

    @Test
    void rejectsOfflineAndMissingOwners() {
        service.award(null, target, 7.5);
        when(owner.isOnline()).thenReturn(false);
        service.award(owner, target, 7.5);
        noAward();
    }

    @Test
    void requiresBridgePermissionAndNativeEligibility() {
        when(owner.hasPermission("mymcmmopet.xp")).thenReturn(false);
        service.award(owner, target, 7.5);
        when(owner.hasPermission("mymcmmopet.xp")).thenReturn(true);
        when(gateway.isEligible(owner, target, 7.5)).thenReturn(false);
        service.award(owner, target, 7.5);
        noAward();
    }

    @Test
    void rejectsCreativeAndSpectatorOwners() {
        when(owner.getGameMode()).thenReturn(GameMode.CREATIVE);
        service.award(owner, target, 7.5);
        when(owner.getGameMode()).thenReturn(GameMode.SPECTATOR);
        service.award(owner, target, 7.5);
        noAward();
    }

    @Test
    void respectsBridgeAndMcmmoWorldExclusions() {
        service(3, false, Set.of("world")).award(owner, target, 7.5);
        when(gateway.isWorldBlacklisted(world)).thenReturn(true);
        service.award(owner, target, 7.5);
        noAward();
    }

    @Test
    void multiplierZeroDisablesXp() {
        service(0, false, Set.of()).award(owner, target, 7.5);
        noAward();
    }

    @Test
    void rejectsInvalidMultiplier() {
        for (double multiplier : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> service(multiplier, false, Set.of()));
        }
    }

    @Test
    void pvpRequiresExplicitOptIn() {
        Player defender = mock(Player.class);
        when(defender.isValid()).thenReturn(true);
        when(defender.getType()).thenReturn(EntityType.PLAYER);
        when(defender.getWorld()).thenReturn(world);
        when(gateway.isEligible(owner, defender, 7.5)).thenReturn(true);
        service.award(owner, defender, 7.5);
        noAward();
        service(3, true, Set.of()).award(owner, defender, 7.5);
        verify(gateway).award(owner, defender, 3);
    }

    @Test
    void rejectsDeadInvalidAndDecorativeTargets() {
        when(target.isDead()).thenReturn(true);
        service.award(owner, target, 7.5);
        when(target.isDead()).thenReturn(false);
        when(target.isValid()).thenReturn(false);
        service.award(owner, target, 7.5);
        ArmorStand stand = mock(ArmorStand.class);
        when(stand.isValid()).thenReturn(true);
        service.award(owner, stand, 7.5);
        noAward();
    }
}

