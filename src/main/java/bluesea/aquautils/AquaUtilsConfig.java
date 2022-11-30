package bluesea.aquautils;

import net.minecraft.server.dedicated.AbstractPropertiesHandler;
import net.minecraft.util.registry.DynamicRegistryManager;

import java.nio.file.Path;
import java.util.Properties;

public class AquaUtilsConfig extends AbstractPropertiesHandler<AquaUtilsConfig> {
    public final boolean kick;

    public AquaUtilsConfig(Properties properties) {
        super(properties);
        this.kick = this.parseBoolean("kick", true);
    }

    @Override
    protected AquaUtilsConfig create(DynamicRegistryManager registryManager, Properties properties) {
        return null;
    }

    public static AquaUtilsConfig load(Path path) {
        return new AquaUtilsConfig(loadProperties(path));
    }
}
