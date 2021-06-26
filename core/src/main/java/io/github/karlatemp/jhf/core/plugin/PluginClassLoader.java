package io.github.karlatemp.jhf.core.plugin;

import io.github.karlatemp.jhf.core.utils.Action0;
import io.github.karlatemp.mxlib.MxLib;
import io.github.karlatemp.mxlib.logger.MLogger;
import io.github.karlatemp.mxlib.utils.StringBuilderFormattable;
import io.github.karlatemp.unsafeaccessor.Root;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Scanner;

public class PluginClassLoader extends URLClassLoader {
    private static final MLogger LOGGER = MxLib.getLoggerFactory().getLogger("JHF.plugin");

    public PluginClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    protected void addURL(URL url) {
        super.addURL(url);
    }

    public static PluginClassLoader PCL(File directory) {
        PluginClassLoader pcl = new PluginClassLoader(PluginClassLoader.class.getClassLoader());
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        LOGGER.verbose(StringBuilderFormattable.by("Loading from ").plusMsg(f));
                        try {
                            pcl.addURL(f.toURI().toURL());
                        } catch (MalformedURLException e) {
                            try {
                                pcl.close();
                            } catch (IOException ioException) {
                                e.addSuppressed(ioException);
                            }
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return pcl;
    }

    public static void loadAndBootstrap(File directory, Action0<PluginClassLoader, Void> callback) throws Throwable {
        LOGGER.verbose(StringBuilderFormattable.by("Loading plugins from ").plusMsg(directory.getAbsolutePath()));
        directory.mkdirs();
        PluginClassLoader pcl = PCL(directory);
        callback.run(pcl);
        Enumeration<URL> mains = pcl.getResources("jhf-main.txt");
        while (mains.hasMoreElements()) {
            URL url = mains.nextElement();
            LOGGER.verbose(StringBuilderFormattable.by("Reading classes from ").plusMsg(url));
            try (Scanner scanner = new Scanner(url.openStream())) {
                while (scanner.hasNextLine()) {
                    String nextLine = scanner.nextLine();
                    if (nextLine.isEmpty()) continue;
                    if (nextLine.charAt(0) == '#') continue;
                    Class<?> cc = pcl.loadClass(nextLine.trim());
                    Root.getTrusted(cc)
                            .findStatic(cc, "launch", MethodType.methodType(void.class))
                            .invoke();
                }
            }
        }
    }
}
