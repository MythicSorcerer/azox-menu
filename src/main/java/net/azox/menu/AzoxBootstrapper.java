package net.azox.menu;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrapper for the AzoxTemplate plugin.
 * Used for pre-initialization logic.
 */
public final class AzoxBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull final BootstrapContext context) {
        // Here you can register custom commands using the new Lifecycle API
        // or perform other early-init tasks.
    }
}
