package io.github.karlatemp.jhf.core.plugin;

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
                    if (f.isFile() && f.getName().equals(".jar")) {
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

    public static PluginClassLoader loadAndBootstrap(File directory) throws Throwable {
        directory.mkdirs();
        PluginClassLoader pcl = PCL(directory);
        Enumeration<URL> mains = pcl.getResources("jhf-main.txt");
        while (mains.hasMoreElements()) {
            try (Scanner scanner = new Scanner(mains.nextElement().openStream())) {
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
        return pcl;
    }
}
