package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.mxlib.MxLib;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class IOUtils {
    public static byte[] readAndClose(InputStream stream) {
        try (InputStream is = stream) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(2048, is.available()));
            byte[] buffer = new byte[2048];
            while (true) {
                int len = is.read(buffer);
                if (len == -1) break;
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (Throwable throwable) {
            MxLib.getLoggerOrStd("JHF.ioutil").error(throwable);
            return new byte[0];
        }
    }
}
