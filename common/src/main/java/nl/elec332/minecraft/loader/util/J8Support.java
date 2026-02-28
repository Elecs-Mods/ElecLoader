package nl.elec332.minecraft.loader.util;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.AbstractMap;
import java.util.Map;

/**
 * Created by Elec332 on 22-02-2026
 */
public class J8Support {

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static byte[] readAllBytes(InputStream stream) throws IOException {
        return ByteStreams.toByteArray(stream);
    }

    // Copied from OpenJDK 21
    public static Path pathOf(URI uri) {
        String scheme =  uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("Missing scheme");
        }

        // check for default provider to avoid loading of installed providers
        if (scheme.equalsIgnoreCase("file")) {
            return FileSystems.getDefault().provider().getPath(uri);
        }

        // try to find provider
        for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider.getPath(uri);
            }
        }

        throw new FileSystemNotFoundException("Provider \"" + scheme + "\" not installed");
    }

    public static FileSystem newFileSystem(Path path) throws IOException {
        return FileSystems.newFileSystem(path, (ClassLoader) null);
    }

}
