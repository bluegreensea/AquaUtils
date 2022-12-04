package bluesea.aquautils;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.dedicated.Settings;

import java.nio.file.Path;
import java.util.Properties;

public class AquaUtilsConfig extends Settings<AquaUtilsConfig> {
    public final boolean kick;

    public AquaUtilsConfig(Properties properties) {
        super(properties);
        this.kick = this.get("kick", true);
    }

    @Override
    protected AquaUtilsConfig reload(RegistryAccess registryAccess, Properties properties) {
        return null;
    }

    public static AquaUtilsConfig load(Path path) {
        return new AquaUtilsConfig(loadFromFile(path));
    }
}
