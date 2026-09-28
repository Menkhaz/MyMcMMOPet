package io.github.takato65.mymcmmopet;

import com.gmail.nossr50.util.MetadataConstants;
import io.github.takato65.mymcmmopet.listeners.EntityListener;
import org.bukkit.entity.Entity;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EntityListenerTest {
    private final Plugin plugin = mock(Plugin.class);
    private final PetAttackResolver resolver = mock(PetAttackResolver.class);
    private final CombatExperienceService experience = mock(CombatExperienceService.class);
    private final LivingEntity target = mock(LivingEntity.class);
    private final Entity source = mock(Entity.class);
    private final Player owner = mock(Player.class);
    private final Map<Plugin, MetadataValue> markers = new HashMap<>();
    private final EntityListener listener = new EntityListener(plugin, resolver, experience);
    private EntityDamageByEntityEvent event;

    @BeforeEach
    void setUp() {
        event = event(source);
        when(resolver.resolve(source)).thenReturn(new PetAttackResolver.Attack(owner));
        when(target.hasMetadata(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE)).thenAnswer(inv -> !markers.isEmpty());
        doAnswer(inv -> {
            MetadataValue value = inv.getArgument(1);
            markers.put(value.getOwningPlugin(), value);
            return null;
        }).when(target).setMetadata(eq(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE), any());
        doAnswer(inv -> { markers.remove(inv.getArgument(1)); return null; })
                .when(target).removeMetadata(eq(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE), any());
    }

    private EntityDamageByEntityEvent event(Entity damager) {
        EntityDamageByEntityEvent result = mock(EntityDamageByEntityEvent.class);
        when(result.getEntity()).thenReturn(target);
        when(result.getDamager()).thenReturn(damager);
        when(result.getFinalDamage()).thenReturn(7.5);
        return result;
    }

    @Test
    void suppressesNativeProcessingAndAwardsOnceUsingFinalDamage() {
        listener.onDamageStart(event);
        assertTrue(target.hasMetadata(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE));
        listener.onDamageStart(event);
        listener.onDamageFinished(event);
        assertFalse(target.hasMetadata(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE));
        listener.onDamageFinished(event);
        verify(experience, times(1)).award(owner, target, 7.5);
    }

    @Test
    void cancelledAttackCleansUpWithoutXp() {
        listener.onDamageStart(event);
        when(event.isCancelled()).thenReturn(true);
        listener.onDamageFinished(event);
        assertTrue(markers.isEmpty());
        verifyNoInteractions(experience);
    }

    @Test
    void attackUncancelledByAnotherPluginIsHandled() {
        when(event.isCancelled()).thenReturn(true);
        listener.onDamageStart(event);
        when(event.isCancelled()).thenReturn(false);
        listener.onDamageFinished(event);
        verify(experience).award(owner, target, 7.5);
    }

    @Test
    void ordinaryAttacksAreUntouched() {
        when(resolver.resolve(source)).thenReturn(null);
        listener.onDamageStart(event);
        listener.onDamageFinished(event);
        assertTrue(markers.isEmpty());
        verifyNoInteractions(experience);
        verify(target, never()).setMetadata(anyString(), any());
    }

    @Test
    void anotherPluginsCustomDamageMarkerIsPreserved() {
        Plugin other = mock(Plugin.class);
        markers.put(other, mock(MetadataValue.class));
        listener.onDamageStart(event);
        listener.onDamageFinished(event);
        assertTrue(markers.containsKey(other));
        verifyNoInteractions(experience);
        verify(target, never()).removeMetadata(anyString(), any());
    }

    @Test
    void markerAddedByAnotherPluginDuringAttackIsPreserved() {
        listener.onDamageStart(event);
        Plugin other = mock(Plugin.class);
        markers.put(other, mock(MetadataValue.class));
        listener.onDamageFinished(event);
        assertEquals(1, markers.size());
        assertTrue(markers.containsKey(other));
    }

    @Test
    void recursiveDamageCannotQueueAnotherAward() {
        listener.onDamageStart(event);
        EntityDamageByEntityEvent recursive = event(source);
        listener.onDamageStart(recursive);
        listener.onDamageFinished(recursive);
        assertTrue(markers.containsKey(plugin));
        listener.onDamageFinished(event);
        verify(experience, times(1)).award(owner, target, 7.5);
    }

    @Test
    void attackingAnotherPetDoesNotAwardXp() {
        when(resolver.isPet(target)).thenReturn(true);
        listener.onDamageStart(event);
        listener.onDamageFinished(event);
        assertTrue(markers.isEmpty());
        verifyNoInteractions(experience);
    }

    @Test
    void disableRemovesOutstandingMarkers() {
        listener.onDamageStart(event);
        listener.close();
        assertTrue(markers.isEmpty());
        listener.onDamageFinished(event);
        verifyNoInteractions(experience);
    }

    @Test
    void resolvesDirectPetWhenDamageIsAttributedToPlayer() {
        DamageSource damageSource = mock(DamageSource.class);
        when(event.getDamageSource()).thenReturn(damageSource);
        when(event.getDamager()).thenReturn(owner);
        when(damageSource.getDirectEntity()).thenReturn(source);
        listener.onDamageStart(event);
        assertTrue(markers.containsKey(plugin));
        listener.onDamageFinished(event);
        verify(experience).award(owner, target, 7.5);
    }
}
