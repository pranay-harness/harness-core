package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorType;
import io.harness.govern.DependencyModule;
import io.harness.references.RefType;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.events.OrchestrationEventHandlerRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.registrar.AdviserRegistrar;
import io.harness.registries.registrar.EngineRegistrar;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StepRegistry;
import io.harness.resolvers.Resolver;
import io.harness.state.Step;
import io.harness.state.StepType;
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
          .put(RegistryType.STEP, StepRegistrar.class)
          .put(RegistryType.ADVISER, AdviserRegistrar.class)
          .put(RegistryType.FACILITATOR, FacilitatorRegistrar.class)
          .put(RegistryType.RESOLVER, ResolverRegistrar.class)
          .put(RegistryType.ORCHESTRATION_EVENT, OrchestrationEventHandlerRegistrar.class)
          .build();

  private static final Set stepClasses = collectClasses(RegistryType.STEP);
  private static final Set adviserClasses = collectClasses(RegistryType.ADVISER);
  private static final Set facilitatorClasses = collectClasses(RegistryType.FACILITATOR);
  private static final Set resolverClasses = collectClasses(RegistryType.RESOLVER);
  private static final Set eventHandlerClasses = collectClasses(RegistryType.ORCHESTRATION_EVENT);

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
  StepRegistry providesStateRegistry(Injector injector) {
    StepRegistry stepRegistry = new StepRegistry();
    injector.injectMembers(stepRegistry);
    stepClasses.forEach(pair -> {
      Pair<StepType, Class<? extends Step>> statePair = (Pair<StepType, Class<? extends Step>>) pair;
      stepRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    return stepRegistry;
  }

  @Provides
  @Singleton
  AdviserRegistry providesAdviserRegistry(Injector injector) {
    AdviserRegistry adviserRegistry = new AdviserRegistry();
    injector.injectMembers(adviserRegistry);
    adviserClasses.forEach(pair -> {
      Pair<AdviserType, Class<? extends Adviser>> adviserPair = (Pair<AdviserType, Class<? extends Adviser>>) pair;
      adviserRegistry.register(adviserPair.getLeft(), adviserPair.getRight());
    });
    return adviserRegistry;
  }

  @Provides
  @Singleton
  ResolverRegistry providesResolverRegistry(Injector injector) {
    ResolverRegistry resolverRegistry = new ResolverRegistry();
    injector.injectMembers(resolverRegistry);
    resolverClasses.forEach(pair -> {
      Pair<RefType, Class<? extends Resolver<?>>> statePair = (Pair<RefType, Class<? extends Resolver<?>>>) pair;
      resolverRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    return resolverRegistry;
  }

  @Provides
  @Singleton
  FacilitatorRegistry providesFacilitatorRegistry(Injector injector) {
    FacilitatorRegistry facilitatorRegistry = new FacilitatorRegistry();
    injector.injectMembers(facilitatorRegistry);
    facilitatorClasses.forEach(pair -> {
      Pair<FacilitatorType, Class<? extends Facilitator>> statePair =
          (Pair<FacilitatorType, Class<? extends Facilitator>>) pair;
      facilitatorRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    return facilitatorRegistry;
  }

  @Provides
  @Singleton
  OrchestrationEventHandlerRegistry providesEventHandlerRegistry(Injector injector) {
    OrchestrationEventHandlerRegistry handlerRegistry = new OrchestrationEventHandlerRegistry();
    injector.injectMembers(handlerRegistry);
    eventHandlerClasses.forEach(pair -> {
      Pair<OrchestrationEventType, Class<? extends OrchestrationEventHandler>> eventHandlerPair =
          (Pair<OrchestrationEventType, Class<? extends OrchestrationEventHandler>>) pair;
      handlerRegistry.register(eventHandlerPair.getLeft(), eventHandlerPair.getRight());
    });
    return handlerRegistry;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of();
  }

  @Override
  protected void configure() {
    // Nothing to configure
  }
}
