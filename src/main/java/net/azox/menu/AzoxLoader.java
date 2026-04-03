package net.azox.menu;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.jetbrains.annotations.NotNull;

/**
 * Loader for the AzoxTemplate plugin.
 * Used for dynamic dependency loading.
 */
public final class AzoxLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull final PluginClasspathBuilder builder) {
        // Here you can add external libraries if needed at runtime
        // builder.addLibrary(new MavenLibraryResolver(...));
    }
}
