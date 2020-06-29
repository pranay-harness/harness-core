package io.harness.morphia;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;

import io.harness.exception.GeneralException;
import io.harness.govern.DependencyModule;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
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
  public Morphia morphia(@Named("morphiaClasses") Set<Class> classes, ObjectFactory objectFactory) {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(objectFactory);
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.map(classes);
    return morphia;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return Collections.emptySet();
  }

  public void testAutomaticSearch() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");
    morphia.mapPackage("io.harness");

    boolean success = true;
    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      if (!morphiaClasses.contains(cls.getClazz())) {
        logger.error(cls.getClazz().toString());
        success = false;
      }
    }

    if (!success) {
      throw new GeneralException("there are classes that are not registered");
    }
  }

  @Override
  protected void configure() {
    if (!inSpring) {
      MapBinder<String, TestExecution> testExecutionMapBinder =
          MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
      testExecutionMapBinder.addBinding("MorphiaRegistration").toInstance(() -> testAutomaticSearch());
    }
  }
}
