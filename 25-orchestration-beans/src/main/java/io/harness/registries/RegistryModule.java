package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.adviser.Adviser;
import io.harness.ambiance.Level;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
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

@OwnedBy(CDC)
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
  StateRegistry providesStateRegistry(Injector injector) {
    StateRegistry stateRegistry = new StateRegistry();
    stateClasses.forEach(clazz -> {
      State state = (State) injector.getInstance(clazz);
      stateRegistry.register(state.getType(), state);
    });
    return stateRegistry;
  }

  @Provides
  @Singleton
  AdviserRegistry providesAdviserRegistry(Injector injector) {
    AdviserRegistry adviserRegistry = new AdviserRegistry();
    adviserClasses.forEach(clazz -> {
      Adviser adviser = (Adviser) injector.getInstance(clazz);
      adviserRegistry.register(adviser.getType(), adviser);
    });
    return adviserRegistry;
  }

  @Provides
  @Singleton
  ResolverRegistry providesResolverRegistry(Injector injector) {
    ResolverRegistry resolverRegistry = new ResolverRegistry();
    resolverClasses.forEach(clazz -> {
      Resolver resolver = (Resolver) injector.getInstance(clazz);
      resolverRegistry.register(resolver.getType(), resolver);
    });
    return resolverRegistry;
  }

  @Provides
  @Singleton
  FacilitatorRegistry providesFacilitatorRegistry(Injector injector) {
    FacilitatorRegistry facilitatorRegistry = new FacilitatorRegistry();
    facilitatorClasses.forEach(clazz -> {
      Facilitator facilitator = (Facilitator) injector.getInstance(clazz);
      facilitatorRegistry.register(facilitator.getType(), facilitator);
    });
    return facilitatorRegistry;
  }

  @Provides
  @Singleton
  LevelRegistry providesLevelRegistry(Injector injector) {
    LevelRegistry levelRegistry = new LevelRegistry();
    levelClasses.forEach(clazz -> {
      Level level = (Level) injector.getInstance(clazz);
      levelRegistry.register(level.getType(), level);
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
