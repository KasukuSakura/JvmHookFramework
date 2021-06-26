package io.github.karlatemp.jhf.core.startup;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.*;

import java.net.URI;
import java.util.ArrayList;

public class JHFCfFactory extends ConfigurationFactory {
    @Override
    protected String[] getSupportedTypes() {
        return new String[0];
    }

    protected Configuration newConf() {
        DefaultConfiguration conf = new DefaultConfiguration();
        MxLibLogger logger = new MxLibLogger();
        conf.addAppender(logger);
        LoggerConfig rootLogger = conf.getRootLogger();
        for (String s : new ArrayList<>(rootLogger.getAppenders().keySet()))
            rootLogger.removeAppender(s);
        rootLogger.addAppender(logger, null, null);
        rootLogger.setLevel(Level.ALL);
        return conf;
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
        return newConf();
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation) {
        return newConf();
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation, ClassLoader loader) {
        return newConf();
    }
}
