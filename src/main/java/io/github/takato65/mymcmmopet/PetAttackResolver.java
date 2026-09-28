package io.github.takato65.mymcmmopet;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.repository.PetManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;
import java.util.UUID;

public class PetAttackResolver {
    // MyPet 4.0.4 tags all pet projectiles, including player-attributed arrows.
    public static final NamespacedKey PROJECTILE_OWNER = new NamespacedKey("mypet", "projectile_owner");
    private final PetManager pets;

    public PetAttackResolver(PetManager pets) {
        this.pets = pets;
    }

    public Attack resolve(Entity source) {
        Pet pet = pets.getPetFromEntity(source);
        if (pet != null) {
            return new Attack(pet.getOwner().getPlayer());
        }
        if (!(source instanceof Projectile projectile)) {
            return null;
        }
        String owner = projectile.getPersistentDataContainer().get(PROJECTILE_OWNER, PersistentDataType.STRING);
        if (owner != null) {
            try {
                return new Attack(Bukkit.getPlayer(UUID.fromString(owner)));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        if (projectile.getShooter() instanceof Entity shooter) {
            pet = pets.getPetFromEntity(shooter);
            if (pet != null) {
                return new Attack(pet.getOwner().getPlayer());
            }
        }
        return null;
    }

    public boolean isPet(Entity entity) {
        return pets.getPetFromEntity(entity) != null;
    }

    // Offline owners still count as recognized pet attacks for native suppression.
    public record Attack(Player owner) { }
}
