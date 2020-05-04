package io.harness.morphia;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.exception.GeneralException;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MorphiaModule extends DependencyProviderModule {
  private static volatile MorphiaModule instance;

  public static MorphiaModule getInstance() {
    if (instance == null) {
      instance = new MorphiaModule();
    }
    return instance;
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
    } catch (Exception e) {
      logger.error("Failed to initialize morphia object factory", e);
      System.exit(1);
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

  public void testAutomaticSearch(Set<Class> testClasses) {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");
    morphia.mapPackage("io.harness");

    final Set<Class> classes = new HashSet<>(morphiaClasses);
    classes.addAll(testClasses);

    boolean success = true;
    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      if (!classes.contains(cls.getClazz())) {
        logger.error(cls.getClazz().toString());
        success = false;
      }
    }

    if (!success) {
      throw new GeneralException("there are classes that are not registered");
    }
  }
}
