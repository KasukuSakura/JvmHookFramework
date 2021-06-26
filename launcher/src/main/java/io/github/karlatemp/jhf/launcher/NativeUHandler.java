package io.github.karlatemp.jhf.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

class NativeUHandler extends URLStreamHandler {
    private final long image;
    private final long maxSize;

    NativeUHandler(long image, long maxSize) {
        this.image = image;
        this.maxSize = maxSize;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String path = u.getPath();
        int ell = path.lastIndexOf('/');
        if (ell != -1) {
            path = path.substring(ell + 1);
        }
        int rangeSplitter = path.indexOf('-');
        if (rangeSplitter == -1) throw new MalformedURLException("Invalid image range: " + path);
        long from, size;
        try {
            from = Long.parseLong(path.substring(0, rangeSplitter));
            size = Long.parseLong(path.substring(rangeSplitter + 1));
        } catch (NumberFormatException e) {
            throw (MalformedURLException) new MalformedURLException("Invalid image range: " + path)
                    .initCause(e);
        }

        if (from < 0) throw new MalformedURLException("Memory out of bounds: from (" + from + ") less than 0");
        if (size < 0) throw new MalformedURLException("Memory out of bounds: size (" + size + ") less than 0");
        if (from + size > maxSize)
            throw new MalformedURLException("Memory out of bounds");
        return new URLConnection(u) {
            @Override
            public void connect() throws IOException {
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new NativeImageInputStream(image + from, size);
            }
        };
    }
}
