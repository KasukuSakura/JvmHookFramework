package io.github.karlatemp.jhf.core.startup;

import io.github.karlatemp.jhf.core.config.JHFConfig;
import io.github.karlatemp.jhf.core.utils.IOUtils;
import io.github.karlatemp.mxlib.MxLib;
import io.github.karlatemp.mxlib.logger.*;
import io.github.karlatemp.mxlib.logger.renders.PrefixSupplierBuilder;
import io.github.karlatemp.mxlib.logger.renders.PrefixedRender;
import io.github.karlatemp.mxlib.logger.renders.SimpleRender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;

public class MxLibLogger extends AbstractAppender {
    private static final Consumer<StringBuilder> printer;
    private static final MessageRender render;
    private static final Map<Level, java.util.logging.Level> mappings = new HashMap<>();

    private static class LogX extends AwesomeLogger.Awesome.Awesome {
        public LogX(String name, Consumer<StringBuilder> printer, MessageRender render) {
            super(name, printer, render);
        }

        @Override
        public boolean isEnabled(java.util.logging.Level level) {
            java.util.logging.Level CONFL = JHFConfig.INSTANCE.loggingLevel;
            if (CONFL != null) return level.intValue() >= CONFL.intValue();
            return true;
        }
    }

    static {
        MessageFactory bmf = JHFConfig.INSTANCE.useAnsiMessage
                ? new AnsiMessageFactory()
                : new RawMessageFactory();

        String prefixConf = new String(
                IOUtils.readAndClose(Objects.requireNonNull(
                        MxLibLogger.class.getResourceAsStream("/jhf/logger/prefix.txt")
                )),
                StandardCharsets.UTF_8
        );
        if (!JHFConfig.INSTANCE.useAnsiMessage) {
            prefixConf = prefixConf.replaceAll("\\u00a7.", "");
        }

        PrefixedRender.PrefixSupplier result = PrefixSupplierBuilder.parseFrom(
                new Scanner(prefixConf)
        );
        PrefixedRender renderX = new PrefixedRender(
                new SimpleRender(bmf), result
        );
        printer = System.out::println;
        render = renderX;
        mappings.put(Level.DEBUG, java.util.logging.Level.FINE);
        mappings.put(Level.ERROR, java.util.logging.Level.SEVERE);
        mappings.put(Level.FATAL, java.util.logging.Level.SEVERE);
        mappings.put(Level.WARN, java.util.logging.Level.WARNING);
        mappings.put(Level.INFO, java.util.logging.Level.INFO);
        mappings.put(Level.TRACE, java.util.logging.Level.FINER);
        MxLib.setLoggerFactory(new MLoggerFactory() {
            @Override
            public @NotNull MLogger getLogger(String name) {
                return new LogX(
                        name,
                        printer,
                        render
                );
            }
        });
        MxLib.setLogger(MxLib.getLoggerFactory().getLogger("TopLevel"));
    }

    protected MxLibLogger() {
        super("MxLib", null, null, false, new Property[0]);
    }

    @Override
    public void append(LogEvent event) {
        MLogger logger = new LogX(
                event.getLoggerName(),
                printer,
                render
        );
        java.util.logging.Level l = mappings.get(event.getLevel());
        if (l == null) l = java.util.logging.Level.INFO;
        if (logger.isEnabled(l)) {
            logger.log(l, event.getMessage().getFormattedMessage(), event.getMessage().getThrowable());
        }
    }

    @Override
    public String getName() {
        return "MxLib";
    }

}
