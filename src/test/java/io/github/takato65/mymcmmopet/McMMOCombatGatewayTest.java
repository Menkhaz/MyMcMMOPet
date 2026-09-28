package io.github.takato65.mymcmmopet;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.CombatUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

class McMMOCombatGatewayTest {
    @Test
    void awardsTamingThroughNativeCombatPipeline() {
        Player owner = mock(Player.class);
        LivingEntity target = mock(LivingEntity.class);
        McMMOPlayer profile = mock(McMMOPlayer.class);
        try (MockedStatic<UserManager> users = mockStatic(UserManager.class);
             MockedStatic<CombatUtils> combat = mockStatic(CombatUtils.class)) {
            users.when(() -> UserManager.getPlayer(owner)).thenReturn(profile);
            new McMMOCombatGateway().award(owner, target, 3);
            combat.verify(() -> CombatUtils.processCombatXP(profile, target, PrimarySkillType.TAMING, 3));
            combat.verifyNoMoreInteractions();
        }
    }

    @Test
    void unloadedMcmmoProfileIsSkipped() {
        try (MockedStatic<UserManager> users = mockStatic(UserManager.class);
             MockedStatic<CombatUtils> combat = mockStatic(CombatUtils.class)) {
            new McMMOCombatGateway().award(mock(Player.class), mock(LivingEntity.class), 3);
            combat.verifyNoInteractions();
        }
    }
}
