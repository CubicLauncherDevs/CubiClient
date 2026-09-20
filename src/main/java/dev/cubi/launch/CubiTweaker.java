package dev.cubi.launch;

import dev.cubi.bridge.Game189;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/** A version entry point, not a Forge mod. Authentication stays with the launcher. */
public final class CubiTweaker implements ITweaker {
    private final List<String> arguments = new ArrayList<String>();

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        arguments.addAll(args);
        option("--gameDir", gameDir == null ? null : gameDir.getAbsolutePath());
        option("--assetsDir", assetsDir == null ? null : assetsDir.getAbsolutePath());
        option("--version", profile);
    }

    private void option(String name, String value) {
        if (value != null && !arguments.contains(name)) {
            arguments.add(name);
            arguments.add(value);
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader loader) {
        // One shared copy of the hooks, including when called by the generated screen.
        loader.addClassLoaderExclusion("dev.cubi.");
        Game189.useClassLoader(loader);
        loader.registerTransformer("dev.cubi.launch.CubiTransformer");
    }

    @Override
    public String getLaunchTarget() { return "net.minecraft.client.main.Main"; }

    @Override
    public String[] getLaunchArguments() { return arguments.toArray(new String[arguments.size()]); }
}
