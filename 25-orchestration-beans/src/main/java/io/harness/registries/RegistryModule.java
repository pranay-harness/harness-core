package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorType;
import io.harness.govern.DependencyModule;
import io.harness.references.RefType;
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
import io.harness.state.StateType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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

  private static final Set stateClasses = collectClasses(RegistryType.STATE);
  private static final Set adviserClasses = collectClasses(RegistryType.ADVISER);
  private static final Set facilitatorClasses = collectClasses(RegistryType.FACILITATOR);
  private static final Set resolverClasses = collectClasses(RegistryType.RESOLVER);
  private static final Set levelClasses = collectClasses(RegistryType.LEVEL);

  private static synchronized Set collectClasses(RegistryType registryType) {
    Set classes = new ConcurrentHashSet<>();
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
    stateClasses.forEach(pair -> {
      Pair<StateType, Class<? extends State>> statePair = (Pair<StateType, Class<? extends State>>) pair;
      stateRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    injector.injectMembers(stateRegistry);
    return stateRegistry;
  }

  @Provides
  @Singleton
  AdviserRegistry providesAdviserRegistry(Injector injector) {
    AdviserRegistry adviserRegistry = new AdviserRegistry();
    adviserClasses.forEach(pair -> {
      Pair<AdviserType, Class<? extends Adviser>> adviserPair = (Pair<AdviserType, Class<? extends Adviser>>) pair;
      adviserRegistry.register(adviserPair.getLeft(), adviserPair.getRight());
    });
    injector.injectMembers(adviserRegistry);
    return adviserRegistry;
  }

  @Provides
  @Singleton
  ResolverRegistry providesResolverRegistry(Injector injector) {
    ResolverRegistry resolverRegistry = new ResolverRegistry();
    resolverClasses.forEach(pair -> {
      Pair<RefType, Class<? extends Resolver<?>>> statePair = (Pair<RefType, Class<? extends Resolver<?>>>) pair;
      resolverRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    injector.injectMembers(resolverRegistry);
    return resolverRegistry;
  }

  @Provides
  @Singleton
  FacilitatorRegistry providesFacilitatorRegistry(Injector injector) {
    FacilitatorRegistry facilitatorRegistry = new FacilitatorRegistry();
    facilitatorClasses.forEach(pair -> {
      Pair<FacilitatorType, Class<? extends Facilitator>> statePair =
          (Pair<FacilitatorType, Class<? extends Facilitator>>) pair;
      facilitatorRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    injector.injectMembers(facilitatorRegistry);
    return facilitatorRegistry;
  }

  @Provides
  @Singleton
  LevelRegistry providesLevelRegistry(Injector injector) {
    LevelRegistry levelRegistry = new LevelRegistry();
    levelClasses.forEach(pair -> {
      Pair<LevelType, Class<? extends Level>> statePair = (Pair<LevelType, Class<? extends Level>>) pair;
      levelRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    injector.injectMembers(levelRegistry);
    return levelRegistry;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of();
  }

  @Override
  protected void configure() {}
}
