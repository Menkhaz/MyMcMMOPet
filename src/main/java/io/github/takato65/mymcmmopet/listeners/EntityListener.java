package io.github.takato65.mymcmmopet.listeners;

import com.gmail.nossr50.util.MetadataConstants;
import io.github.takato65.mymcmmopet.CombatExperienceService;
import io.github.takato65.mymcmmopet.PetAttackResolver;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import java.util.IdentityHashMap;
import java.util.Map;

public class EntityListener implements Listener, AutoCloseable {
    private final Plugin plugin;
    private final PetAttackResolver resolver;
    private final CombatExperienceService experience;
    private final Map<EntityDamageByEntityEvent, PetAttackResolver.Attack> pending = new IdentityHashMap<>();

    public EntityListener(Plugin plugin, PetAttackResolver resolver, CombatExperienceService experience) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.experience = experience;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageStart(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)
                || pending.containsKey(event)
                || target.hasMetadata(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE)) {
            return;
        }
        // Paper damage sources can credit the owner as the causing entity while
        // retaining the pet/projectile as the direct entity. Prefer that source.
        Entity direct = event.getDamageSource() == null ? null : event.getDamageSource().getDirectEntity();
        PetAttackResolver.Attack attack = direct == null ? null : resolver.resolve(direct);
        if (attack == null && direct != event.getDamager()) {
            attack = resolver.resolve(event.getDamager());
        }
        if (attack != null) {
            pending.put(event, attack);
            // mcMMO checks this before its HIGHEST handler. Suppress native
            // wolf/Archery/Tridents XP and abilities for this pet attack only.
            // Ownership is ours, so cleanup cannot remove another plugin's marker.
            target.setMetadata(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE, new FixedMetadataValue(plugin, true));
        }
    }

    // Cancelled attacks must also have their temporary marker removed.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamageFinished(EntityDamageByEntityEvent event) {
        PetAttackResolver.Attack attack = pending.remove(event);
        if (attack == null) {
            return;
        }
        LivingEntity target = (LivingEntity) event.getEntity();
        target.removeMetadata(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE, plugin);
        if (!event.isCancelled() && !resolver.isPet(target)) {
            experience.award(attack.owner(), target, event.getFinalDamage());
        }
    }

    @Override
    public void close() {
        for (EntityDamageByEntityEvent event : pending.keySet()) {
            event.getEntity().removeMetadata(MetadataConstants.METADATA_KEY_CUSTOM_DAMAGE, plugin);
        }
        pending.clear();
    }
}
