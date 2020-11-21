package io.harness.morphia;

import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.morphia.MorphiaRegistrar.putClass;

import io.harness.exception.GeneralException;
import io.harness.exception.UnexpectedException;
import io.harness.reflection.CodeUtils;
import io.harness.testing.TestExecution;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedClass;

@Slf4j
public class MorphiaModule extends AbstractModule {
  private static volatile MorphiaModule instance;

  public static MorphiaModule getInstance() {
    if (instance == null) {
      instance = new MorphiaModule();
    }
    return instance;
  }

  private MorphiaModule() {}

  @Provides
  @Named("morphiaClasses")
  @Singleton
  Set<Class> classes(Set<Class<? extends MorphiaRegistrar>> registrars) {
    Set<Class> classes = new HashSet<>();
    try {
      for (Class clazz : registrars) {
        Constructor<?> constructor = clazz.getConstructor();
        final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();
        morphiaRegistrar.registerClasses(classes);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new GeneralException("Failed initializing morphia", e);
    }

    return classes;
  }

  @Provides
  @Named("morphiaInterfaceImplementersClasses")
  @Singleton
  Map<String, Class> collectMorphiaInterfaceImplementers(Set<Class<? extends MorphiaRegistrar>> registrars) {
    Map<String, Class> map = new ConcurrentHashMap<>();
    MorphiaRegistrarHelperPut h = (name, clazz) -> putClass(map, "io.harness." + name, clazz);
    MorphiaRegistrarHelperPut w = (name, clazz) -> putClass(map, "software.wings." + name, clazz);

    try {
      for (Class clazz : registrars) {
        Constructor<?> constructor = clazz.getConstructor();
        final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();

        morphiaRegistrar.registerImplementationClasses(h, w);
      }
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new GeneralException("Failed to initialize MorphiaInterfaceImplementers", e);
    }

    return map;
  }

  @Provides
  @Singleton
  public Morphia morphia(@Named("morphiaClasses") Set<Class> classes,
      @Named("morphiaClasses") Map<Class, String> customCollectionName, ObjectFactory objectFactory, Injector injector,
      Set<Class<? extends TypeConverter>> morphiaConverters) {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(objectFactory);
    morphia.getMapper().getOptions().setMapSubPackages(true);

    Set<Class> classesCopy = new HashSet<>(classes);

    try {
      Method method =
          morphia.getMapper().getClass().getDeclaredMethod("addMappedClass", MappedClass.class, boolean.class);
      method.setAccessible(true);

      for (Map.Entry<Class, String> entry : customCollectionName.entrySet()) {
        classesCopy.remove(entry.getKey());

        HMappedClass mappedClass = new HMappedClass(entry.getValue(), entry.getKey(), morphia.getMapper());

        method.invoke(morphia.getMapper(), mappedClass, Boolean.TRUE);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new UnexpectedException("We cannot add morphia MappedClass", e);
    }
    morphia.map(classesCopy);
    morphiaConverters.forEach(
        converter -> morphia.getMapper().getConverters().addConverter(injector.getInstance(converter)));
    return morphia;
  }

  public void testAutomaticSearch(Provider<Set<Class>> classesProvider) {
    Morphia morphia;
    try {
      morphia = new Morphia();
      morphia.getMapper().getOptions().setMapSubPackages(true);
      morphia.mapPackage("software.wings");
      morphia.mapPackage("io.harness");
    } catch (NoClassDefFoundError error) {
      ignoredOnPurpose(error);
      return;
    }

    log.info("Checking {} classes", morphia.getMapper().getMappedClasses().size());

    boolean success = true;
    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      if (!classesProvider.get().contains(cls.getClazz())) {
        log.error("Class {} is missing in the registrars", cls.getClazz().getName());
        success = false;
      }
    }

    if (!success) {
      throw new GeneralException("there are classes that are not registered");
    }
  }

  public void testAllRegistrars(Provider<Set<Class<? extends MorphiaRegistrar>>> registrarsProvider) {
    try {
      for (Class clazz : registrarsProvider.get()) {
        Constructor<?> constructor = null;
        constructor = clazz.getConstructor();
        final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();

        if (CodeUtils.isTestClass(clazz)) {
          continue;
        }

        log.info("Checking registrar {}", clazz.getName());
        morphiaRegistrar.testClassesModule();
        morphiaRegistrar.testImplementationClassesModule();
      }
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new UnexpectedException("Unexpected exception while constructing registrar", e);
    }
  }

  @Override
  protected void configure() {
    MapBinder<String, TestExecution> testExecutionMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
    if (!binder().currentStage().name().equals("TOOL")) {
      Provider<Set<Class>> providerClasses =
          getProvider(Key.get(new TypeLiteral<Set<Class>>() {}, Names.named("morphiaClasses")));
      testExecutionMapBinder.addBinding("Morphia test registration")
          .toInstance(() -> testAutomaticSearch(providerClasses));
      Provider<Set<Class<? extends MorphiaRegistrar>>> providerRegistrars =
          getProvider(Key.get(new TypeLiteral<Set<Class<? extends MorphiaRegistrar>>>() {}));
      testExecutionMapBinder.addBinding("Morphia test registrars")
          .toInstance(() -> testAllRegistrars(providerRegistrars));
    }
  }
}
