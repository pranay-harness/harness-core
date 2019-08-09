package io.harness.mongo;

import com.google.inject.Provides;
import com.google.inject.name.Named;

import io.harness.govern.ProviderModule;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Set;

@Slf4j
public class MorphiaModule extends ProviderModule {
  private static synchronized Set<Class> collectMorphiaClasses() {
    Set<Class> morphiaClasses = new ConcurrentHashSet<>();

    try {
      Reflections reflections = new Reflections("io.harness.serializer.morphia");
      for (Class clazz : reflections.getSubTypesOf(MorphiaRegistrar.class)) {
        Constructor<?> constructor = clazz.getConstructor();
        final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();

        morphiaRegistrar.register(morphiaClasses);
      }
    } catch (Exception e) {
      logger.error("Failed to initialize morphia object factory", e);
      System.exit(1);
    }

    return morphiaClasses;
  }

  private Set<Class> morphiaClasses = collectMorphiaClasses();

  @Provides
  @Named("morphiaClasses")
  Set<Class> classes() {
    return morphiaClasses;
  }
}
