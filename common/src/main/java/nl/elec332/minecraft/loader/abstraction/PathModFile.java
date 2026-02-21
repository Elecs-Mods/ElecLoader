package nl.elec332.minecraft.loader.abstraction;

import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.IModFileResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 17-09-2023
 */
public class PathModFile extends AbstractModFile implements IModFile.FileLister {

    public static PathModFile of(PathModFile... files) {
        return of(Arrays.stream(files).flatMap(f -> f.root.stream()).collect(Collectors.toSet()));
    }

    public static PathModFile of(URL... urls) {
        return of(Arrays.stream(urls).map(u -> {
            try {
                return Path.of(u.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));
    }

    public static PathModFile of(URI... root) {
        return of(Arrays.stream(root)
                .map(Path::of)
                .collect(Collectors.toList()));
    }

    public static PathModFile of(Path... root) {
        return of(Arrays.stream(root)
                .collect(Collectors.toList()));
    }

    public static PathModFile of(Collection<Path> root) {
        Set<Path> paths = root.stream().collect(Collectors.toUnmodifiableSet());
        final Set<Consumer<Consumer<Path>>> accessors = new HashSet<>();
        for (Path path : paths) {
            try(Stream<Path> files = Files.walk(path)) {
                if (files.count() > 1 || path.getFileSystem() == FileSystems.getDefault()) {
                    accessors.add(c -> c.accept(path));
                } else {
                    accessors.add(c -> {
                        try(var fs = FileSystems.newFileSystem(path)) {
                            c.accept(fs.getPath("/"));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return new PathModFile(paths, c -> accessors.forEach(uc -> uc.accept(c)));
    }

    private PathModFile(Set<Path> root, Consumer<Consumer<Path>> pathAccessor) {
        this.root = root;
        this.pathAccessor = pathAccessor;
    }

    private final Set<Path> root;
    private final Consumer<Consumer<Path>> pathAccessor;
    private final Set<String> files = new HashSet<>(), files_ = Collections.unmodifiableSet(files);

    @Override
    public void scanFile(String startFolder, IModFileResource.Visitor consumer) {
        PathModFileResource resource = new PathModFileResource(null, true);
        pathAccessor.accept(root -> {
            iterate(startFolder, root, (p, a) -> {
                if (a.isRegularFile()) {
                    if (!scanned) {
                        this.files.add(root.relativize(p).toString());
                        if (p.getFileName().toString().endsWith(".class")) {
                            this.classPaths.add(root.relativize(p).toString());
                        }
                    }
                    return true;
                }
                if (p.getNameCount() == 0) {
                    return false;
                }
                if (!scanned) {
                    p = root.relativize(p);
                    if (a.isDirectory() && !(p.startsWith("assets") || p.startsWith("data") || p.startsWith("META-INF"))) {
                        this.pack.add(p.toString());
                    }
                }
                return false;
            }, p -> {
                resource.path = p;
                consumer.visit(root.relativize(p).toString(), resource);
            });
        });
        if (!scanned) {
            this.pack.remove("");
            scanned = true;
        }
    }

    public static void iterate(String startFolder, Path root, BiPredicate<Path, BasicFileAttributes> matcher, Consumer<Path> consumer) {
        Path start;
        if (startFolder == null || startFolder.isEmpty()) {
            start = root;
        } else {
            start = root.resolve(startFolder).normalize();
        }
        if (!start.startsWith(root)) {
            return;
        }
        if (!Files.isDirectory(start)) {
            return;
        }
        try (Stream<Path> files = Files.find(root, Integer.MAX_VALUE, matcher)) {
            files.forEach(consumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<IModFileResource> findResource(String file) {
        for (Path root : root) {
            Path path = root.resolve(file.replace("/", root.getFileSystem().getSeparator()));
            if (Files.exists(path)) {
                return Optional.of(new PathModFileResource(path, false));
            }
        }
        return Optional.empty();
    }

    @Override
    protected void scanFile() {
        scanFile((p, r) -> {});
    }

    @Override
    public Set<String> getFiles() {
        checkScanned();
        return files_;
    }

    @Nullable
    @Override
    public String getComparableRootPath() {
        if (root.size() != 1) {
            return null;
        }
        URI uri = root.iterator().next().toUri();
        String ret = uri.getPath();
        if (ret != null && !ret.isEmpty()) {
            return ret;
        }
        try {
            return uri.toURL().getPath();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getRootFileString() {
        return root.stream().map(Path::toUri).map(Objects::toString).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return getRootFileString();
    }

}
