package dev.arda.origins;

import com.starshootercity.OriginsAddon;
import com.starshootercity.abilities.types.Ability;
import dev.arda.origins.abilities.Abilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Origins-Reborn addon adding Tolkien-themed abilities (namespace "arda"). */
public class ArdaOrigins extends OriginsAddon {
    private final List<Ability> abilities = Abilities.all();

    @Override
    public @NotNull String getNamespace() {
        return "arda";
    }

    @Override
    public @NotNull List<Ability> getRegisteredAbilities() {
        return abilities;
    }

    /** Called at the start of onEnable by OriginsAddon. */
    @Override
    public void onRegister() {
        // Once a second, update every ability that needs periodic work.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (Ability ability : abilities) {
                    if (ability instanceof Abilities.Tickable tickable) {
                        tickable.tick(player);
                    }
                }
            }
        }, 20L, 20L);
    }
}
