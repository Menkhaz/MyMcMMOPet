# MyMCMMOPet

Bridge for **MyPet 4** and **mcMMO 2.3.001**. Pet melee and projectile attacks
award the owner's **Taming** XP independently of the pet's own experience.

## Supported build

Compiled and regression-tested against:

- MyPet `4.0.4-alpha-local` from the supplied MyPet project.
- mcMMO `2.3.001` from the supplied server JAR.
- Paper API `1.21.11-R0.1-SNAPSHOT`, Java 21.

Use Paper 1.21.11 or later with Java 21 or later (newer Minecraft releases may
require Java 25). MyPet 3 is unsupported. This bridge does not declare Folia
support. Compatibility with other MyPet/mcMMO builds needs verification.

## Installation

1. Stop the server and remove the old `MyMCMMOPet-1.1.jar`, if installed.
2. Copy `target/MyMCMMOPet-2.0.0.jar` into `plugins/`, alongside MyPet and mcMMO.
3. Start the server and check for the bridge's enabled message.
4. Edit `plugins/MyMCMMOPet/config.yml` as needed, then restart the server.

Do not install another pet-to-mcMMO XP bridge at the same time.

## Configuration

```yaml
xp-multiplier: 3.0
allow-pvp-xp: false
disabled-worlds: []
```

The multiplier applies to mcMMO combat XP; `3.0` matches mcMMO's normal wolf
Taming multiplier. It is not a flat XP-per-hit amount. Set `0` to disable XP
from bridge-handled pet attacks. Negative or non-finite values disable the plugin
with a configuration error.

Players need `mymcmmopet.xp` (granted by default) and mcMMO's Taming skill
permission. Offline owners, unloaded mcMMO profiles, creative/spectator owners,
cancelled attacks, zero damage, decorative targets, and attacks on other MyPets
award no bridge XP. Both world blacklists apply. PvP requires an explicit bridge
opt-in and mcMMO's own PvP/party eligibility settings.

The bridge checks mcMMO skill eligibility and WorldGuard restrictions, then uses
mcMMO's native combat XP calculation. This keeps mcMMO's mob XP values,
spawn-source/anti-exploit multipliers, health-loss calculation, combat HP ceiling,
XP modifiers, party sharing, cancellable XP events, and level progression.

## Duplicate XP prevention

MyPet 4 uses ordinary Bukkit mobs. A wolf can otherwise trigger mcMMO's native
Taming handler; pet arrows and tridents can trigger its player-shooter handlers.
The bridge recognizes pets using MyPet's entity index and tagged projectiles
using `mypet:projectile_owner`. It also reads Paper's direct damage source when
the causing entity is the owner.

For a recognized pet hit, the bridge temporarily adds its own mcMMO custom-damage
metadata marker before mcMMO's combat handler. It removes that marker at MONITOR,
including on cancelled hits, and submits at most one Taming XP request for the
event. Other plugins' markers are preserved. Ordinary player attacks are untouched.

Bridge-handled pet hits do not trigger mcMMO wolf combat abilities or Archery/
Tridents XP and combat effects. MyPet's own combat skills and pet XP remain in
charge of the pet.

mcMMO calculates combat damage by observing target health asynchronously. Its
normal timing limitations still apply when several sources damage the same mob
before its XP task runs; this bridge does not replace that native calculation.

## Building

Requires JDK 21+ and Maven 3.9+. To build against the exact installed plugin JARs:

```powershell
mvn -Plocal-jars '-Dmypet.jar=C:/path/to/MyPet.jar' '-Dmcmmo.jar=C:/path/to/mcMMO.jar' clean verify
```

The supplied MyPet project's `api/build/libs/api.jar` can also be used as the
MyPet dependency. Paths must be absolute. Neither plugin is bundled in the
bridge JAR. `mvn verify` uses the configured published APIs instead; availability
of the MyPet snapshot depends on its upstream repository.

## Validation

Automated tests cover entity/projectile ownership, direct damage-source
attribution, cancellation and uncancellation, marker cleanup and ownership,
recursive and repeated event handling, XP opt-outs, game modes, world filters,
PvP opt-in, multiplier validation, and native Taming XP delegation.

Before production use, verify on your server with a non-operator survival player:

- A melee pet, wolf pet, and ranged pet award Taming XP on eligible mobs.
- Pet arrows/tridents do not also award Archery/Tridents XP.
- Cancelled/protected hits award no XP and later ordinary attacks still work.
- Spawned/otherwise restricted mobs follow your mcMMO experience settings.
- Pet kills award XP; normal player attacks and vanilla wolves still behave normally.
- Bridge permission denial, multiplier `0`, and world exclusions prevent pet XP.

Compilation and automated tests do not constitute a live server gameplay test.
