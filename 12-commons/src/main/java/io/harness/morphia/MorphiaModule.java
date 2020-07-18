package io.harness.morphia;

import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.harness.exception.GeneralException;
import io.harness.exception.UnexpectedException;
import io.harness.govern.DependencyModule;
import io.harness.reflection.CodeUtils;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MorphiaModule extends DependencyModule {
  private static volatile MorphiaModule instance;

  public static MorphiaModule getInstance() {
    if (instance == null) {
      instance = new MorphiaModule();
    }
    return instance;
  }

  private boolean inSpring;

  public MorphiaModule() {
    inSpring = false;
  }

  public MorphiaModule(boolean inSpring) {
    this.inSpring = inSpring;
  }

  private static synchronized Set<Class> collectMorphiaClasses() {
    Set<Class> morphiaClasses = new ConcurrentHashSet<>();

    try {
      Reflections reflections = new Reflections("io.harness.serializer.morphia");
      for (Class clazz : reflections.getSubTypesOf(MorphiaRegistrar.class)) {
        Constructor<?> constructor = clazz.getConstructor();
        final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();

        morphiaRegistrar.registerClasses(morphiaClasses);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new GeneralException("Failed initializing morphia", e);
    }

    return morphiaClasses;
  }

  private static Set<Class> morphiaClasses = collectMorphiaClasses();

  @Provides
  @Named("morphiaClasses")
  @Singleton
  Set<Class> classes() {
    return morphiaClasses;
  }

  @Provides
  @Singleton
  public Morphia morphia(@Named("morphiaClasses") Set<Class> classes,
      @Named("morphiaClasses") Map<Class, String> customCollectionName, ObjectFactory objectFactory) {
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
    return morphia;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return Collections.emptySet();
  }

  public void testAutomaticSearch() {
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

    logger.info("Checking {} classes", morphia.getMapper().getMappedClasses().size());

    boolean success = true;
    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      if (!morphiaClasses.contains(cls.getClazz())) {
        logger.error(cls.getClazz().getName());
        success = false;
      }
    }

    if (!success) {
      throw new GeneralException("there are classes that are not registered");
    }
  }

  public void testAllRegistrars() {
    Reflections reflections = new Reflections("io.harness.serializer.morphia");
    try {
      for (Class clazz : reflections.getSubTypesOf(MorphiaRegistrar.class)) {
        Constructor<?> constructor = null;
        constructor = clazz.getConstructor();
        final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();

        if (CodeUtils.isTestClass(clazz)) {
          continue;
        }

        logger.info("Checking registrar {}", clazz.getName());
        morphiaRegistrar.testClassesModule();
        morphiaRegistrar.testImplementationClassesModule();
      }
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new UnexpectedException("Unexpected exception while constructing registrar", e);
    }
  }

  @Override
  protected void configure() {
    MapBinder.newMapBinder(binder(), Class.class, String.class, Names.named("morphiaClasses"));

    if (!inSpring) {
      MapBinder<String, TestExecution> testExecutionMapBinder =
          MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
      testExecutionMapBinder.addBinding("Morphia test registration").toInstance(() -> testAutomaticSearch());
      testExecutionMapBinder.addBinding("Morphia test registrars").toInstance(() -> testAllRegistrars());
    }
  }
}
