package rszip;

import io.github.karlatemp.jhf.launcher.CSWHelper;
import io.github.karlatemp.jhf.launcher.FlattenJarFile;

import java.io.*;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ZipBinary {

    public static void main(String[] args) throws Throwable {
        File base = new File("../agent/src/main/resources/io/github/karlatemp/jhf/agent");

        File dmp = new File(base, "dmp.bin");
        File inf = new File(base, "dmp.inf");
        dmp.getParentFile().mkdirs();
        ZipLauncher.run(new File(base, "launcher.bin"));

        FlattenJarFile fjf = new FlattenJarFile().initialize();
        BufferedOutputStream dumpOs = new BufferedOutputStream(new FileOutputStream(dmp));
        long dmpPointer = 0;

        for (String s : args) {
            File rs = new File(s);
            String artifactName;
            String artifactVersion;
            {
                String name = rs.getName();
                int splitter = name.lastIndexOf('-');
                artifactName = name.substring(0, splitter);
                artifactVersion = name.substring(splitter + 1, name.length() - 4/* .jar */);
            }
            String moduleName;
            if (rs.getPath().replace('\\', '/').contains("/build/libs/")) {
                moduleName = "jvmhookframework." + artifactName;
            } else {
                moduleName = artifactName;
            }
            FlattenJarFile.ModuleInfo mif = new FlattenJarFile.ModuleInfo().initialize();
            fjf.modules.add(mif);
            mif.name = moduleName;

            try (JarFile jarFile = new JarFile(rs)) {
                mif.manifest = jarFile.getInputStream(jarFile.getEntry("META-INF/MANIFEST.MF")).readAllBytes();
                for (ZipEntry entry : II.of(jarFile.entries().asIterator())) {
                    if (entry.getName().startsWith("META-INF")) continue;
                    if (entry.getName().endsWith(".class")) {
                        String n = entry.getName();
                        int lst = n.lastIndexOf('/');
                        if (lst == -1 || lst == 0) continue;
                        n = n.substring(0, lst);
                        mif.packages.add(n);
                    }
                }
                mif.packages = new ArrayList<>(new HashSet<>(mif.packages));
                for (JarEntry jarEntry : (II<JarEntry>) new II(jarFile.entries().asIterator())) {
                    if (jarEntry.isDirectory()) continue;
                    Certificate[] certificates = jarEntry.getCertificates();
                    int[] crtx;
                    if (certificates == null || certificates.length == 0) {
                        crtx = CSWHelper.EMPTY_INT_ARRAY;
                    } else {
                        crtx = new int[certificates.length];
                        for (int i = 0; i < crtx.length; i++) {
                            Certificate c = certificates[i];
                            int inx = fjf.certificates.indexOf(c);
                            if (inx == -1) {
                                crtx[i] = fjf.certificates.size();
                                fjf.certificates.add(certificates[i]);
                            } else {
                                crtx[i] = inx;
                            }
                        }
                    }
                    FlattenJarFile.ResPair resource = new FlattenJarFile.ResPair().initialize();
                    resource.signers = crtx;
                    resource.pointer = dmpPointer;
                    resource.name = jarEntry.getName();
                    fjf.resources.add(resource);

                    try (InputStream is = jarFile.getInputStream(jarEntry)) {
                        byte[] buffer = new byte[20480];
                        while (true) {
                            int ln = is.read(buffer);
                            if (ln == -1) break;
                            dumpOs.write(buffer, 0, ln);
                            dmpPointer += ln;
                        }
                    }

                    resource.size = dmpPointer - resource.pointer;

                }
            }

            var mxlib = new FlattenJarFile.ModuleInfo().initialize();
            mxlib.name = "mxlib";
            {
                var iterator = fjf.modules.iterator();
                while (iterator.hasNext()) {
                    var mf = iterator.next();
                    if (mf.name.startsWith("mxlib")) {
                        iterator.remove();
                        mxlib.packages.addAll(mf.packages);
                    }
                }
            }
            mxlib.packages = new ArrayList<>(new HashSet<>(mxlib.packages));
            fjf.modules.add(mxlib);
        }

        fjf.imageSize = dmpPointer;

        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(inf)));
        fjf.write(dos);
        dos.flush();
        dos.close();
        dumpOs.flush();
        dumpOs.close();
    }

    static class II<T> implements Iterable<T> {
        static <T> II<T> of(Iterator<T> itr) {
            return new II<>(itr);
        }

        private final Iterator<T> itr;

        public II(Iterator<T> itr) {
            this.itr = itr;
        }

        @Override
        public Iterator<T> iterator() {
            return itr;
        }
    }
}
