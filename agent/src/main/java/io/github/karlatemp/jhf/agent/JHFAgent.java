package io.github.karlatemp.jhf.agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class JHFAgent {
    private final static AtomicBoolean RUNNING = new AtomicBoolean();

    public static void premain(String option, Instrumentation instrumentation) throws Exception {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }

        byte[] bin = loadData();
        DataInputStream bis = new DataInputStream(new ByteArrayInputStream(bin));
        int size = bis.readInt();
        Map<String, byte[]> data = new HashMap<>(size);
        while (size-- > 0) {
            String key = bis.readUTF();
            byte[] x = new byte[bis.readInt()];
            bis.readFully(x);
            data.put(key, x);
        }
        new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = data.get(name.replace('.', '/') + ".class");
                if (bytes == null) throw new ClassNotFoundException(name);
                return defineClass(name, bytes, 0, bytes.length, null);
            }

            @Override
            public InputStream getResourceAsStream(String name) {
                InputStream rs = super.getResourceAsStream(name);
                if (rs == null) rs = JHFAgent.class.getResourceAsStream(name);
                return rs;
            }
        }.loadClass("io.github.karlatemp.jhf.launcher.JHFLauncher")
                .getMethod("launch", Instrumentation.class)
                .invoke(null, instrumentation);
    }

    private static byte[] loadData() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream is = JHFAgent.class.getResourceAsStream("launcher.bin")) {
            byte[] buffer = new byte[20480];
            while (true) {
                int ln = is.read(buffer);
                if (ln == -1) break;
                bos.write(buffer, 0, ln);
            }
        }
        return bos.toByteArray();
    }
}
