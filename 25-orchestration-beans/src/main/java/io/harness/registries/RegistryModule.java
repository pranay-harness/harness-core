package io.harness.registries;

import static org.joor.Reflect.on;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.adviser.Adviser;
import io.harness.ambiance.Level;
import io.harness.facilitate.Facilitator;
import io.harness.govern.DependencyModule;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.level.LevelRegistry;
import io.harness.registries.registrar.AdviserRegistrar;
import io.harness.registries.registrar.EngineRegistrar;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.registries.registrar.LevelRegistrar;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.registries.registrar.StateRegistrar;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StateRegistry;
import io.harness.resolvers.Resolver;
import io.harness.state.State;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

@Slf4j
public class RegistryModule extends DependencyModule {
  private static RegistryModule instance;

  public static synchronized RegistryModule getInstance() {
    if (instance == null) {
      instance = new RegistryModule();
    }
    return instance;
  }

  private static final Map<RegistryType, Class<? extends EngineRegistrar>> registryTypeClassMap =
      ImmutableMap.<RegistryType, Class<? extends EngineRegistrar>>builder()
          .put(RegistryType.STATE, StateRegistrar.class)
          .put(RegistryType.ADVISER, AdviserRegistrar.class)
          .put(RegistryType.FACILITATOR, FacilitatorRegistrar.class)
          .put(RegistryType.RESOLVER, ResolverRegistrar.class)
          .put(RegistryType.LEVEL, LevelRegistrar.class)
          .build();

  private static Set<Class> stateClasses = collectClasses(RegistryType.STATE);
  private static Set<Class> adviserClasses = collectClasses(RegistryType.ADVISER);
  private static Set<Class> facilitatorClasses = collectClasses(RegistryType.FACILITATOR);
  private static Set<Class> resolverClasses = collectClasses(RegistryType.RESOLVER);
  private static Set<Class> levelClasses = collectClasses(RegistryType.LEVEL);

  private static synchronized Set<Class> collectClasses(RegistryType registryType) {
    Set<Class> classes = new ConcurrentHashSet<>();
    try {
      Reflections reflections = new Reflections("io.harness.registrars");
      for (Class clazz : reflections.getSubTypesOf(registryTypeClassMap.get(registryType))) {
        Constructor<?> constructor = clazz.getConstructor();
        final EngineRegistrar registrar = (EngineRegistrar) constructor.newInstance();
        registrar.register(classes);
      }
    } catch (Exception e) {
      logger.error("Failed to Initialize Engine Registrar", e);
      System.exit(1);
    }
    return classes;
  }

  @Provides
  @Singleton
  StateRegistry providesStateRegistry() {
    StateRegistry stateRegistry = new StateRegistry();
    stateClasses.forEach(clazz -> {
      State state = on(clazz).create().get();
      stateRegistry.register(state.getType(), clazz);
    });
    return stateRegistry;
  }

  @Provides
  @Singleton
  AdviserRegistry providesAdviserRegistry() {
    AdviserRegistry adviserRegistry = new AdviserRegistry();
    adviserClasses.forEach(clazz -> {
      Adviser adviser = on(clazz).create().get();
      adviserRegistry.register(adviser.getType(), clazz);
    });
    return adviserRegistry;
  }

  @Provides
  @Singleton
  ResolverRegistry providesResolverRegistry() {
    ResolverRegistry resolverRegistry = new ResolverRegistry();
    resolverClasses.forEach(clazz -> {
      Resolver resolver = on(clazz).create().get();
      resolverRegistry.register(resolver.getType(), clazz);
    });
    return resolverRegistry;
  }

  @Provides
  @Singleton
  FacilitatorRegistry providesFacilitatorRegistry() {
    FacilitatorRegistry facilitatorRegistry = new FacilitatorRegistry();
    facilitatorClasses.forEach(clazz -> {
      Facilitator facilitator = on(clazz).create().get();
      facilitatorRegistry.register(facilitator.getType(), clazz);
    });
    return facilitatorRegistry;
  }

  @Provides
  @Singleton
  LevelRegistry providesLevelRegistry() {
    LevelRegistry levelRegistry = new LevelRegistry();
    levelClasses.forEach(clazz -> {
      Level level = on(clazz).create().get();
      levelRegistry.register(level.getType(), clazz);
    });
    return levelRegistry;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of();
  }

  @Override
  protected void configure() {}
}
