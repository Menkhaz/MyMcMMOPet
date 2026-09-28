package io.github.takato65.mymcmmopet;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.PetManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PetAttackResolverTest {
    private final PetManager pets = mock(PetManager.class);
    private final PetAttackResolver resolver = new PetAttackResolver(pets);
    private final Player owner = mock(Player.class);

    private void bind(Entity entity) {
        Pet pet = mock(Pet.class);
        MyPetPlayer petOwner = mock(MyPetPlayer.class);
        when(pets.getPetFromEntity(entity)).thenReturn(pet);
        when(pet.getOwner()).thenReturn(petOwner);
        when(petOwner.getPlayer()).thenReturn(owner);
    }

    private Projectile projectile(String uuid) {
        Projectile result = mock(Projectile.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(result.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.get(PetAttackResolver.PROJECTILE_OWNER, PersistentDataType.STRING)).thenReturn(uuid);
        return result;
    }

    @Test
    void resolvesMeleePetFromManager() {
        Entity source = mock(Entity.class);
        bind(source);
        assertSame(owner, resolver.resolve(source).owner());
        assertTrue(resolver.isPet(source));
    }

    @Test
    void resolvesPetProjectileEvenWhenShooterIsPlayer() {
        UUID uuid = UUID.randomUUID();
        Projectile source = projectile(uuid.toString());
        when(source.getShooter()).thenReturn(owner);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(owner);
            assertSame(owner, resolver.resolve(source).owner());
        }
    }

    @Test
    void offlineTaggedProjectileRemainsRecognized() {
        Projectile source = projectile(UUID.randomUUID().toString());
        try (MockedStatic<Bukkit> ignored = mockStatic(Bukkit.class)) {
            assertNotNull(resolver.resolve(source));
            assertNull(resolver.resolve(source).owner());
        }
    }

    @Test
    void malformedOwnerTagDoesNotThrow() {
        assertNull(resolver.resolve(projectile("invalid")));
    }

    @Test
    void fallsBackToPetMobShooter() {
        Projectile source = projectile(null);
        org.bukkit.entity.Mob shooter = mock(org.bukkit.entity.Mob.class);
        bind(shooter);
        when(source.getShooter()).thenReturn(shooter);
        assertSame(owner, resolver.resolve(source).owner());
    }

    @Test
    void ordinaryPlayerProjectilesAndMobsAreIgnored() {
        Projectile source = projectile(null);
        when(source.getShooter()).thenReturn(owner);
        assertNull(resolver.resolve(source));
        assertNull(resolver.resolve(mock(Entity.class)));
    }
}
