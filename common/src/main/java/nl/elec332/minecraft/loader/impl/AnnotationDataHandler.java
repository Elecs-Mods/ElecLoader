package nl.elec332.minecraft.loader.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import nl.elec332.minecraft.loader.api.discovery.AnnotationDataProcessor;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataProcessor;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 29-10-2016.
 */
enum AnnotationDataHandler {

    INSTANCE;

    AnnotationDataHandler() {
        asmLoaderMap = Maps.newHashMap();
        validStates = ImmutableList.copyOf(EnumSet.complementOf(EnumSet.of(ModLoadingStage.PRE_CONSTRUCT)));
        sideOnlyCache = Maps.newHashMap();
    }

    private final Map<ModLoadingStage, Multimap<IModContainer, IAnnotationDataProcessor>> asmLoaderMap;
    private final List<ModLoadingStage> validStates;
    private IAnnotationDataHandler asmDataHelper;

    private final Map<String, Boolean> sideOnlyCache;
    private Predicate<String> hasWrongSideOnly;
    private Set<IAnnotationData> annotationDataSet;

    IAnnotationDataHandler identify(Set<IModFile> modFiles, BiPredicate<String, IAnnotationDataHandler> hasWrongSideOnly_) {
        if (this.asmDataHelper != null) {
            throw new UnsupportedOperationException();
        }
        this.hasWrongSideOnly = cls -> AnnotationDataHandler.this.sideOnlyCache.computeIfAbsent(cls, name -> hasWrongSideOnly_.test(name, asmDataHelper));
        annotationDataSet = new HashSet<>();

        final Map<IModFile, SetMultimap<Type, IAnnotationData>> annotationDataF = modFiles.stream()
                .collect(Collectors.toMap(Function.identity(), mf -> {
                    SetMultimap<Type, IAnnotationData> ret = HashMultimap.create();
                    mf.getAnnotations().stream()
                            .filter(Objects::nonNull)
                            .forEach(ad -> {
                                IAnnotationData annotationData = new AnnotationData(ad, mf);
                                if (annotationData.getAnnotationName().startsWith("Ljava/lang") || annotationData.getAnnotationName().startsWith("Ljavax/annotation")) {
                                    return;
                                }
                                Type annType = ad.annotationType();
                                ret.put(annType, annotationData);
                            });
                    return ret;
                }));
        final SetMultimap<Type, IAnnotationData> annotationData = HashMultimap.create();
        annotationDataF.values().forEach(annotationData::putAll);
        final Map<String, SetMultimap<Type, IAnnotationData>> classAnnotationData = new HashMap<>();
        annotationData.values().forEach(a -> classAnnotationData.computeIfAbsent(a.getClassName(), s -> HashMultimap.create()).put(a.getAnnotationType(), a));
        annotationData.values().forEach(a -> {
            if (!annotationDataSet.add(a)) {
                throw new IllegalStateException();
            }
        });

        this.asmDataHelper = new IAnnotationDataHandler() {

            @Override
            public Set<IAnnotationData> getAnnotationList(Type annotationType) {
                Set<IAnnotationData> ret = annotationData.get(annotationType);
                return ret == null ? ImmutableSet.of() : Collections.unmodifiableSet(ret);
            }

            @Override
            public boolean hasWrongSideOnlyAnnotation(String clazz) {
                return hasWrongSideOnly.test(clazz);
            }

            @NotNull
            @Override
            public Function<Type, Set<IAnnotationData>> getAnnotationsForClass(String clazz) {
                return type -> Optional.ofNullable(classAnnotationData.get(clazz)).map(t -> t.get(type)).orElse(ImmutableSet.of());
            }

            @NotNull
            @Override
            public Function<Type, Set<IAnnotationData>> getAnnotationsFor(IModFile file) {
                return type -> Optional.ofNullable(annotationDataF.get(file)).map(t -> t.get(type)).orElse(ImmutableSet.of());
            }

            @Override
            public Function<Type, Set<IAnnotationData>> getAnnotationsFor(IModContainer mc) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Map<IModContainer, Set<IAnnotationData>> getModdedAnnotationMap(Type annotationType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public IModContainer findOwner(IAnnotationData annotationData) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String findOwnerName(IAnnotationData annotationData) {
                throw new UnsupportedOperationException();
            }

        };

        return this.asmDataHelper;
    }

    void attribute(Collection<IModContainer> modList) {
        for (ModLoadingStage state : validStates) {
            asmLoaderMap.put(state, HashMultimap.create());
        }

        Map<String, IModContainer> pck = Maps.newTreeMap((o1, o2) -> {
            if (o2.contains(o1)) {
                return 1;
            }
            if (o1.contains(o2)) {
                return -1;
            }
            return o1.compareTo(o2);
        });

        modList.forEach(m -> m.getOwnedPackages().forEach(p -> pck.put(p, m)));

        Set<String> packSorted = pck.keySet();
        Function<String, IModContainer> packageOwners = pck::get;

        Function<IAnnotationData, IModContainer> modSearcher = annotationData -> {
            if (annotationData.getClassName().startsWith("net.minecraft.") || (annotationData.getClassName().startsWith("mcp.") && !annotationData.getClassName().contains("mobius"))) {
                return Objects.requireNonNull(IModLoader.INSTANCE.getModContainer("minecraft"));//FMLHelper.getModList().getModContainerById(DefaultModInfos.minecraftModInfo.getModId()).orElseThrow(NullPointerException::new);
            }
            IModFile owner = annotationData.getFile();
            if (owner.getMods().size() == 1) {
                return Objects.requireNonNull(IModLoader.INSTANCE.getModContainer(owner.getMods().get(0).getModId()));
            }
            //TODO: Debug remove, allow null below?
            System.out.println(annotationData);
            System.out.println(owner);
            System.out.println(owner.getMods());
            String pack = packSorted.stream().filter(s -> annotationData.getClassName().contains(s)).findFirst().orElseThrow(RuntimeException::new);
            System.out.println("--------");
            return packageOwners.apply(pack);
        };

        final Map<String, SetMultimap<Type, IAnnotationData>> annotationDataM = Maps.newHashMap();
        final Map<IModContainer, SetMultimap<Type, IAnnotationData>> annotationDataM2 = Maps.newHashMap();

        annotationDataSet.forEach(annotationData -> {
            IModContainer mc = modSearcher.apply(annotationData);
            if (mc != null) {
                annotationDataM2.computeIfAbsent(mc, s -> {
                    SetMultimap<Type, IAnnotationData> ret = HashMultimap.create();
                    annotationDataM.put(mc.getModId(), ret);
                    return ret;
                }).put(annotationData.getAnnotationType(), annotationData);
//                annotationDataM.computeIfAbsent(mc.getModId(), s -> HashMultimap.create()).put(annotationData.getAnnotationType(), annotationData);
            }
        });
        annotationDataSet = null;

        final IAnnotationDataHandler rootHandler = asmDataHelper;
        this.asmDataHelper = new IAnnotationDataHandler() {

            @Override
            public Set<IAnnotationData> getAnnotationList(Type annotationType) {
                return rootHandler.getAnnotationList(annotationType);
            }

            @Override
            public boolean hasWrongSideOnlyAnnotation(String clazz) {
                return rootHandler.hasWrongSideOnlyAnnotation(clazz);
            }

            @NotNull
            @Override
            public Function<Type, Set<IAnnotationData>> getAnnotationsFor(IModFile file) {
                return rootHandler.getAnnotationsFor(file);
            }

            @Override
            public @NotNull Function<Type, Set<IAnnotationData>> getAnnotationsForClass(String clazz) {
                return rootHandler.getAnnotationsForClass(clazz);
            }

            @Override
            public Function<Type, Set<IAnnotationData>> getAnnotationsFor(IModContainer mc) {
                return type -> Optional.ofNullable(annotationDataM.get(mc.getModId())).map(t -> t.get(type)).orElse(ImmutableSet.of());
            }

            @Override
            public Map<IModContainer, Set<IAnnotationData>> getModdedAnnotationMap(Type annotationType) {
                return annotationDataM2.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().get(annotationType)));
            }

            @Override
            public IModContainer findOwner(IAnnotationData annotationData) {
                return modSearcher.apply(annotationData);
            }

            @Override
            public String findOwnerName(IAnnotationData annotationData) {
                IModFile owner = annotationData.getFile();
                if (owner.getMods().size() == 1) {
                    //Small speed optimization, as normally there also has to be a search from id -> container.
                    //Since the vast majority of files contain only one mod this will almost always be faster.
                    return owner.getMods().get(0).getModId();
                }
                return findOwner(annotationData).getModId();
            }

        };
    }

    void preProcess() {
        Map<Map.Entry<Integer, Map.Entry<IAnnotationDataProcessor, IModContainer>>, ModLoadingStage[]> dataMap = Maps.newTreeMap(Comparator.comparing((Function<Map.Entry<Integer, Map.Entry<IAnnotationDataProcessor, IModContainer>>, Integer>) Map.Entry::getKey).reversed().thenComparing(Object::hashCode));

        for (Map.Entry<IModContainer, Set<IAnnotationData>> entry : asmDataHelper.getModdedAnnotationMap(AnnotationDataProcessor.class).entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalStateException("Failed to find mods for annotations: " + entry.getValue());
            }
            for (IAnnotationData data : entry.getValue()) {
                if (data.hasWrongSideOnlyAnnotation()) {
                    continue;
                }
                boolean eb = false;
                Class<?> clazz;
                try {
                    clazz = Class.forName(data.getClassName(), true, IModLoader.INSTANCE.getModClassLoader());
                } catch (ClassNotFoundException e) {
                    //Do nothing, class is probably annotated with @SideOnly
                    continue;
                }
                if (clazz == null) {
                    continue;
                }
                if (clazz.isAnnotationPresent(AnnotationDataProcessor.class)) {
                    AnnotationDataProcessor annData = clazz.getAnnotation(AnnotationDataProcessor.class);
                    ModLoadingStage[] ls = annData.value();
                    int importance = annData.importance();
                    if (clazz.isEnum()) {
                        for (Object e : clazz.getEnumConstants()) {
                            if (e instanceof IAnnotationDataProcessor) {
                                dataMap.put(Map.entry(importance, Map.entry((IAnnotationDataProcessor) e, entry.getKey())), ls);
                            }
                        }
                        eb = true;
                    } else {
                        Object o;
                        try {
                            o = clazz.getConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException("Error invocating annotated IASMData class: " + data.getClassName(), e);
                        }
                        if (o instanceof IAnnotationDataProcessor) {
                            dataMap.put(Map.entry(importance, Map.entry((IAnnotationDataProcessor) o, entry.getKey())), ls);
                        }
                    }
                }

                if (!eb) {
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.isAnnotationPresent(AnnotationDataProcessor.class)) {
                            Object obj;
                            try {
                                obj = field.get(null);
                            } catch (Exception e) {
                                continue; //Not static
                            }
                            if (obj instanceof IAnnotationDataProcessor) {
                                AnnotationDataProcessor annData = field.getAnnotation(AnnotationDataProcessor.class);
                                dataMap.put(Map.entry(annData.importance(), Map.entry((IAnnotationDataProcessor) obj, entry.getKey())), annData.value());
                            }
                        }
                    }
                }
            }

        }

        for (Map.Entry<Map.Entry<Integer, Map.Entry<IAnnotationDataProcessor, IModContainer>>, ModLoadingStage[]> entry : dataMap.entrySet()) {
            ModLoadingStage[] hS = entry.getValue();
            if (hS == null || hS.length == 0) {
                throw new IllegalArgumentException("Invalid ModLoadingStage parameters: Null or empty array; For " + entry.getKey().getValue().getClass());
            }
            for (ModLoadingStage state : hS) {
                if (!validStates.contains(state)) {
                    throw new IllegalArgumentException("Invalid ModLoadingStage parameter: " + state + "; For " + entry.getKey().getValue().getClass());
                }
                asmLoaderMap.get(state).put(entry.getKey().getValue().getValue(), entry.getKey().getValue().getKey());
            }
        }
    }

    void process(ModLoadingStage state) {
        if (validStates.contains(state)) {
            Multimap<IModContainer, IAnnotationDataProcessor> dataProcessors = asmLoaderMap.get(state);
            dataProcessors.forEach((mc, dataProcessor) -> ((ElecModContainer) mc).enqueueDeferredWork(state, () -> dataProcessor.processASMData(asmDataHelper, state)));
            asmLoaderMap.remove(state);
        } else {
            throw new IllegalArgumentException();
        }
    }

//    @APIHandlerInject(weight = 1)
//    public void injectASMHelper(IAPIHandler apiHandler) {
//        apiHandler.inject(this.asmDataHelper, IAnnotationDataHandler.class);
//    }

    private class AnnotationData implements IAnnotationData {

        private AnnotationData(IModFile.RawAnnotationData asmData, IModFile file) {
            this.asmData = Preconditions.checkNotNull(asmData);
            this.modFile = file;
            this.isField = asmData.memberName().indexOf('(') == -1;
            this.isClass = asmData.memberName().indexOf('.') != -1;
            this.annotationInfo = Collections.unmodifiableMap(asmData.annotationData());
        }

        private final IModFile modFile;
        private final IModFile.RawAnnotationData asmData;
        private final Map<String, Object> annotationInfo;
        private final boolean isField, isClass;
        private Class<?> clazz;
        private Field field;
        private String methodName, methodParams;
        private Method method;
        private Type[] paramTypes;
        private Class<?>[] params;
        private Boolean sideOnly;

        @Override
        public IModFile getFile() {
            return modFile;
        }

        @Override
        public Type getAnnotationType() {
            return asmData.annotationType();
        }

        @Override
        public ElementType getTargetType() {
            return asmData.targetType();
        }

        @Override
        public Map<String, Object> getAnnotationInfo() {
            return annotationInfo;
        }

        @Override
        public Class<?> loadClass() {
            if (clazz != null) {
                return clazz;
            }
            try {
                return clazz = Class.forName(getClassName(), true, IModLoader.INSTANCE.getModClassLoader());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Type getClassType() {
            return asmData.classType();
        }

        @Override
        public String getMemberName() {
            return asmData.memberName();
        }

        @Override
        public boolean isClass() {
            return isClass;
        }

        @Override
        public boolean isField() {
            return isField && !isClass;
        }

        @Override
        public String getFieldName() {
            if (!isField()) {
                throw new IllegalAccessError();
            }
            return asmData.memberName();
        }

        @Override
        public Field getField() {
            if (field != null) {
                return field;
            }
            if (!isField()) {
                throw new IllegalAccessError();
            }
            try {
                field = loadClass().getDeclaredField(getFieldName());
                field.setAccessible(true);
                return field;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Class<?> getFieldType() {
            return getField().getType();
        }

        @Override
        public boolean isMethod() {
            return !isField && !isClass;
        }

        @Override
        public String getMethodName() {
            if (!isMethod()) {
                throw new IllegalAccessError();
            }
            if (methodName == null) {
                String targetName = asmData.memberName();
                int i = targetName.indexOf('('), i2 = targetName.indexOf(')');
                methodName = targetName.substring(0, i);
                if (i2 - i == 1) {
                    methodParams = "";
                    paramTypes = new Type[0];
                    params = new Class[0];
                } else {
                    methodParams = targetName.substring(i, i2 + 1);
                }
            }
            return methodName;
        }

        @Override
        public Method getMethod() {
            if (method != null) {
                return method;
            }
            if (!isMethod()) {
                throw new IllegalAccessError();
            }
            try {
                method = loadClass().getDeclaredMethod(getMethodName(), getMethodParameters());
                method.setAccessible(true);
                return method;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Type[] getMethodParameterTypes() {
            if (methodParams == null) {
                getMethodName();
            }
            if (paramTypes != null) {
                return paramTypes;
            }
            return paramTypes = Type.getArgumentTypes(methodParams);
        }

        @Override
        public Class<?>[] getMethodParameters() {
            if (params != null) {
                return params;
            }
            if (!isMethod()) {
                throw new IllegalAccessError();
            }
            Type[] p = getMethodParameterTypes();
            Class<?>[] ret = new Class<?>[p.length];
            try {
                for (int i = 0; i < p.length; i++) {
                    ret[i] = Class.forName(p[i].getClassName());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return params = ret;
        }

        @Override
        public boolean hasWrongSideOnlyAnnotation() {
            if (sideOnly == null) {
                sideOnly = hasWrongSideOnly.test(getClassName());
            }
            return sideOnly;
        }

        @Override
        public String toString() {
            return " Annotation:" + getAnnotationName()
                    + " Class:" + getClassName()
                    + " Field name:" + (isField() ? getFieldName() : "-")
                    + " Method name:" + (isMethod() ? getMethodName() : "-")
                    + " Annotation data:" + getAnnotationInfo();
        }

    }
}
