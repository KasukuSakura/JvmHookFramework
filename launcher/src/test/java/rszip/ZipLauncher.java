package rszip;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ZipLauncher {
    public static void emit(Map<String, byte[]> data, Path src) throws Exception {
        try (var stream = Files.walk(src)) {
            var itr = stream.iterator();
            while (itr.hasNext()) {
                var next = itr.next();
                if (Files.isDirectory(next)) continue;
                data.put(
                        src.relativize(next).toString().replace('\\', '/'),
                        Files.newInputStream(next).readAllBytes()
                );
            }
        }
    }

    public static void run(File fileOut) throws Exception {
        fileOut.getParentFile().mkdirs();
        var out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileOut)));
        Map<String, byte[]> data = new HashMap<>();

        emit(data, new File("build/classes/java/main").toPath());
        emit(data, new File("build/classes/java/j9").toPath());

        out.writeInt(data.size());
        for (Map.Entry<String, byte[]> entry : data.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
        }
        out.close();
    }
}
