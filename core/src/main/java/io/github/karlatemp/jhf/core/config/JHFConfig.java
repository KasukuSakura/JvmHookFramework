package io.github.karlatemp.jhf.core.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.logging.Level;

@ConfigSerializable
public class JHFConfig {
    public static HoconConfigurationLoader newLoader(ConfigurationOptions options, Path path) {
        return HoconConfigurationLoader.builder()
                .prettyPrinting(true)
                .emitComments(true)
                .headerMode(HeaderMode.PRESET)
                .defaultOptions(options)
                .path(path)
                .build();
    }

    public static final File workingDir = new File(".jvm-hook-framework");
    public static JHFConfig INSTANCE = new JHFConfig();

    public static final TypeSerializerCollection EXTRACT_SERIALIZERS = TypeSerializerCollection.builder().register(
            Level.class, new TypeSerializer<Level>() {
                @Override
                public Level deserialize(Type type, ConfigurationNode node) throws SerializationException {
                    try {
                        return Level.parse(node.getString("INFO"));
                    } catch (Exception e) {
                        throw new SerializationException(e);
                    }
                }

                @Override
                public void serialize(Type type, @Nullable Level obj, ConfigurationNode node) throws SerializationException {
                    if (obj == null) {
                        node.set("INFO");
                        return;
                    }
                    node.set(obj.getName());
                }
            })
            .build();


    public static final HoconConfigurationLoader LOADER = newLoader(
            ConfigurationOptions.defaults()
                    .shouldCopyDefaults(true)
                    .header("Configuration of Jvm Hook Framework")
                    .serializers(builder -> builder.registerAll(EXTRACT_SERIALIZERS))
            ,
            new File(workingDir, "config.conf").toPath()
    );

    public static void reload() throws Throwable {
        CommentedConfigurationNode configurationNode = LOADER.load();
        CommentedConfigurationNode mirror = configurationNode.copy();
        JHFConfig config = configurationNode.get(JHFConfig.class);
        if (!configurationNode.equals(mirror)) {
            LOADER.save(configurationNode);
        }
        if (config != null)
            INSTANCE = config;
    }

    public static void main(String[] args) throws Throwable {
        reload();
    }

    @Comment("Should hidden stack track when thrown a exception from JHF to injected classes")
    public boolean hiddenStackTrack = true;

    @Comment("Should hidden all stack track after first hidden stack track")
    public boolean hiddenAll = false;

    @Comment("Use ansi logging or not")
    public boolean useAnsiMessage = true;

    @Comment("Logging level, options: OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL")
    public Level loggingLevel = Level.ALL;

    @Comment("Save generated classes")
    public boolean saveGeneratedClasses = false;
}
