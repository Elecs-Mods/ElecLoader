package nl.elec332.minecraft.loader.abstraction;

import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;

import java.util.*;

public abstract class AbstractModFile implements IModFile {

    protected boolean scanned = false;
    protected boolean fullyScanned = false;

    final List<IModMetaData> containedMods = new ArrayList<>(), containedMods_ = Collections.unmodifiableList(this.containedMods);
    final Set<RawAnnotationData> annotationData = new HashSet<>();
    final Set<ClassData> classData = new HashSet<>();

    protected final Set<String> pack = new HashSet<>();
    protected final Set<String> classPaths = new HashSet<>();

    private final Set<RawAnnotationData> annotationData_ = Collections.unmodifiableSet(this.annotationData);
    private final Set<ClassData> classData_ = Collections.unmodifiableSet(this.classData);
    private final Set<String> classPaths_ = Collections.unmodifiableSet(this.classPaths);
    private final Set<String> pack_ = Collections.unmodifiableSet(this.pack);

    @Override
    public final List<IModMetaData> getMods() {
        return this.containedMods_;
    }

    @Override
    public final Set<RawAnnotationData> getAnnotations() {
        if (!fullyScanned) {
            throw new IllegalStateException();
        }
        return this.annotationData_;
    }

    @Override
    public final Set<ClassData> getClasses() {
        if (!fullyScanned) {
            throw new IllegalStateException();
        }
        return this.classData_;
    }

    protected abstract void scanFile();

    protected final void checkScanned() {
        if (!this.scanned) {
            scanFile();
            this.scanned = true;
        }
    }

    @Override
    public final Set<String> getClassFiles() {
        checkScanned();
        return this.classPaths_;
    }

    @Override
    public final Set<String> getPackages() {
        checkScanned();
        return this.pack_;
    }

}
