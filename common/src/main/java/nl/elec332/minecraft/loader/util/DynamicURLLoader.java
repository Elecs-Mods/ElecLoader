package nl.elec332.minecraft.loader.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Created by Elec332 on 04-02-2024
 */
public class DynamicURLLoader extends URLStreamHandler {

    public static URL create(String name, byte[] stream) {
        return create(Collections.singletonMap('/' + name.replace('.', '/') + ".class", stream));
    }

    public static URL create(Map<String, byte[]> providers) {
        try {
            return new URL(UUID.randomUUID().toString(), null, -1, "/", new DynamicURLLoader(providers));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unexpected error creating URL", e);
        }
    }

    public static Map.Entry<URL, BiConsumer<String, byte[]>> create() {
        try {
            final DynamicURLLoader loader = new DynamicURLLoader(new HashMap<>());
            return J8Support.entry(new URL(UUID.randomUUID().toString(), null, -1, "/", loader), loader.providers::put);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unexpected error creating URL", e);
        }
    }

    private DynamicURLLoader(Map<String, byte[]> providers) {
        this.providers = providers;
    }

    private final Map<String, byte[]> providers;

    @Override
    protected URLConnection openConnection(URL url) {
        if (!providers.containsKey(url.getPath())){
            return null;
        }
        return new CasualConnection(url, providers.get(url.getPath()));
    }

    private static final class CasualConnection extends URLConnection {
        private final byte[] realStream;

        public CasualConnection(URL url, byte[] realStream) {
            super(url);
            this.realStream = realStream;
        }

        @Override
        public void connect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(realStream);
        }

        @Override
        public Permission getPermission() {
            return null;
        }

    }

}
