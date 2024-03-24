package nl.elec332.minecraft.loader.abstraction;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.distmarker.OnlyIn;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class AbstractModLoader<T> implements IModLoader {

    public AbstractModLoader() {
        this.pathFiles = new HashSet<>();
        this.pathFiles_ = Collections.unmodifiableSet(this.pathFiles);
        this.modFiles = new HashSet<>();
        this.modFiles_ = Collections.unmodifiableSet(this.modFiles);
        this.mods = new HashSet<>();
        this.mods_ = Collections.unmodifiableSet(this.mods);
        this.modFinder = new HashMap<>();
        this.up = new HashSet<>();
        this.up_ = Collections.unmodifiableSet(this.up);
        this.packageInfo = new HashSet<>();
        this.modMetas = new HashMap<>();
    }

    protected final Set<IModFile> pathFiles, modFiles;
    protected final Set<IModContainer> mods;
    protected final Map<String, IModContainer> modFinder;
    protected final Set<String> up;

    private final Set<IModFile> pathFiles_, modFiles_;
    private final Set<IModContainer> mods_;
    private final Set<String> up_;
    private final Map<String, IModMetaData> modMetas;

    private Set<Map.Entry<IModMetaData, Set<String>>> packageInfo;

    protected void identifyPackages(ModFileMapper mfm, Set<? extends IModFile> libraries) {
        if (packageInfo == null || !packageInfo.isEmpty()) {
            throw new IllegalStateException();
        }
        if (libraries != null) {
            this.pathFiles.addAll(libraries);
        }
        this.modFiles.addAll(mfm.getFiles());



        Set<String> pcks = new HashSet<>();
        SetMultimap<T, String> mpcks = HashMultimap.create();
        mfm.forEach((m, file) -> {
            file.getPackages().forEach(p -> {
                if (pcks.add(p)) {
                    mpcks.put(m, p);
                } else {
                    up.add(p);
                    wl: //This doesn't exist, please look away...
                    while (true) {
                        for (var list : mpcks.asMap().values()) {
                            if (list.remove(p)) {
                                continue wl;
                            }
                        }
                        break;
                    }
                }
            });
        });
        mfm.forEach((mod, file) -> {
            IModMetaData meta = Objects.requireNonNull(getModMeta(mod, file), "meta");
            ((AbstractModFile) file).containedMods.add(meta);
            if (meta.getModFile() != file) {
                throw new UnsupportedOperationException();
            }
            String mId = meta.getModId();
            if (modMetas.containsKey(mId)) {
                throw new IllegalStateException("Duplicate mod ID: " + mId);
            }
            modMetas.put(mId, meta);
            packageInfo.add(Map.entry(meta,  ImmutableSet.copyOf(mpcks.get(mod))));
        });
        scanAnnotations();
    }

    public final void fillModContainers() {
        packageInfo.forEach(e -> {
            IModContainer mc = Objects.requireNonNull(createModContainer(e.getKey(), e.getValue()));
            if (mc.getFile() != e.getKey().getModFile() || mc.getModMetadata() != e.getKey()) {
                throw new UnsupportedOperationException();
            }
            ElecModLoader.verifyModContainer(mc, e.getValue());
            if (!this.mods.add(mc)) {
                throw new RuntimeException();
            }
            if (this.modFinder.containsKey(mc.getModId())) {
                throw new RuntimeException();
            }
            this.modFinder.put(mc.getModId(), mc);
        });
        packageInfo = null;
    }

    protected void scanAnnotations() {
        getModFiles().stream()
                .map(f -> (AbstractModFile) f)
                .forEach(f -> {
                    scanAnnotations(f, f.classData, f.annotationData);
                    f.fullyScanned = true;
                });
    }

    protected void scanAnnotations(IModFile source, Set<IModFile.ClassData> classes, Set<IModFile.RawAnnotationData> annotations) {
        source.scanFile(path -> {
            try (InputStream in = Files.newInputStream(path)) {
                AnnotationFinder av = new AnnotationFinder();
                ClassReader cr = new ClassReader(in);
                cr.accept(av, 0);
                av.accumulate(classes, annotations);

            } catch (IOException | IllegalArgumentException e) {
                //nbc
            }
        });
    }

    abstract protected IModMetaData getModMeta(T container, IModFile file);

    protected IModContainer createModContainer(final IModMetaData meta, final Set<String> packs) {
        IModContainer mc = ElecModLoader.getModLoader().getModContainer(meta.getModId());
        if (mc != null) {
            return mc;
        }
        return new IModContainer() {

            @Override
            public IModMetaData getModMetadata() {
                return meta;
            }

            @Override
            public Set<String> getOwnedPackages() {
                return packs;
            }

            @Override
            public String toString() {
                return toInfoString();
            }

        };
    }

    @Nullable
    @Override
    public IModMetaData getModMetaData(String id) {
        return this.modMetas.get(id);
    }

    @Override
    public boolean hasWrongSideOnly(String clazz, IAnnotationDataHandler annotationData) {
        Set<IAnnotationData> ad = annotationData.getAnnotationsForClass(clazz).apply(org.objectweb.asm.Type.getType(OnlyIn.class));
        for (var a : ad) {
            if (!a.isClass()) {
                continue;
            }
            IAnnotationData.EnumHolder enumHolder = (IAnnotationData.EnumHolder) a.getAnnotationInfo().get("value");
            if (!ElecModLoader.getDist().toString().equals(enumHolder.value())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final Set<IModContainer> getMods() {
        if (packageInfo != null) {
            throw new UnsupportedOperationException();
        }
        return this.mods_;
    }

    @Override
    public final Set<IModFile> getModFiles() {
        return this.modFiles_;
    }

    @Override
    public final Set<IModFile> getLibraries() {
        return this.pathFiles_;
    }

    @Override
    public final IModContainer getModContainer(String id) {
        if (packageInfo != null) {
            throw new UnsupportedOperationException();
        }
        return this.modFinder.get(id);
    }

    @Override
    public final Set<String> getUnownedPackages() {
        return this.up_;
    }

    protected class ModFileMapper {

        public ModFileMapper() {
        }

        private final Map<T, Integer> modMapper = new HashMap<>();
        private final Map<Integer, IModFile> fileMapper = new HashMap<>();

        public void add(T mod, IModFile file) {
//            for (var mf : fileMapper.entrySet()) {
//                if (mf.getValue() instanceof IModFile.FileLister && file instanceof IModFile.FileLister) {
//                    if (((IModFile.FileLister) mf.getValue()).getFiles().containsAll(((IModFile.FileLister) file).getFiles())) {
//                        modMapper.put(mod, mf.getKey());
//                        return;
//                    }
//                } else {
//                    if (mf.getValue().getPackages().containsAll(file.getPackages())) {
//                        modMapper.put(mod, mf.getKey());
//                        return;
//                    }
//                }
//
//            }
            int hash = file.hashCode();
            modMapper.put(mod, hash);
            fileMapper.put(hash, file);
            modMapper.values().forEach(i -> Objects.requireNonNull(fileMapper.get(i)));
        }

        public boolean reduceLibraries(URL url) {
            try {
                String p = url.getPath();
                for (IModFile file : getFiles()) {
                    String p2s = file.getComparableRootPath();
                    if (p2s == null) {
                        continue;
                    }

                    //Please send help
                    String[] split = p2s.replace(p, "|").split("\\|");
                    if (p.equals(p2s) || (split.length == 2 && split[0].equals("file://") && split[1].equals("!/"))) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return true;
            }
        }

        public void checkFiles(BiFunction<T, IModFile, IModFile> checker) {
            modMapper.forEach((mc, i) -> {
                IModFile original = Objects.requireNonNull(fileMapper.get(i));
                IModFile file = checker.apply(mc, original);
                if (file == null) {
                    throw new UnsupportedOperationException();
                }
                if (file != original) {
                    fileMapper.put(i, file);
                }
            });
        }

        public void checkLibraries(Set<? extends IModFile> classPath) {
            Objects.requireNonNull(classPath).remove(null);
            checkDuplicates(fileMapper);

            for (var entry : fileMapper.entrySet()) {
                final IModFile.FileLister file = (IModFile.FileLister) entry.getValue();
                boolean[] r = {false};
                classPath.removeIf(cl2 -> {
                    IModFile.FileLister cl = (IModFile.FileLister) cl2;
                    if (file.getFiles().containsAll(cl.getFiles())) {
                        if (file.getFiles().size() <= cl.getFiles().size()) {
                            if (file.getFiles().size() != cl.getFiles().size()) {
                                throw new RuntimeException();
                            }
                            if (r[0] && !(AbstractModLoader.this.isDevelopmentEnvironment() && cl.getPackages().contains("net/minecraft/util") && "Quilt".equals(AbstractModLoader.this.getModLoaderName()))) {
                                System.out.println(file);
                                System.out.println(cl);
                                throw new RuntimeException();
                            }
//                            entry.setValue(cl);
                        }
                        r[0] = true;
                        return true;
                    }
                    return false;
                });
            }
            checkDuplicates(fileMapper);
        }

        public void forEach(BiConsumer<T, IModFile> consumer) {
            modMapper.forEach((m, i) -> consumer.accept(m, Objects.requireNonNull(fileMapper.get(i))));
        }

        protected Collection<IModFile> getFiles() {
            return fileMapper.values();
        }

    }

    protected static <M> void checkDuplicates(Map<M, IModFile> modFiles) {
        for (M m1 : modFiles.keySet()) {
            for (M m2 : modFiles.keySet()) {
                if (m1 != m2 && ((IModFile.FileLister) modFiles.get(m1)).getFiles().equals(((IModFile.FileLister) modFiles.get(m2)).getFiles())) {
                    throw new RuntimeException();
                }
            }
        }
    }

}
