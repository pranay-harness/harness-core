package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

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

import java.util.HashSet;
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

  @Provides
  @Singleton
  StepRegistry providesStateRegistry(Injector injector, Map<String, StepRegistrar> stepRegistrarMap) {
    Set classes = new HashSet<>();
    stepRegistrarMap.values().forEach(stepRegistrar -> { stepRegistrar.register(classes); });
    StepRegistry stepRegistry = new StepRegistry();
    injector.injectMembers(stepRegistry);
    classes.forEach(pair -> {
      Pair<StepType, Class<? extends Step>> statePair = (Pair<StepType, Class<? extends Step>>) pair;
      stepRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    return stepRegistry;
  }

  @Provides
  @Singleton
  AdviserRegistry providesAdviserRegistry(Injector injector, Map<String, AdviserRegistrar> adviserRegistrarMap) {
    Set classes = new HashSet<>();
    adviserRegistrarMap.values().forEach(adviserRegistrar -> { adviserRegistrar.register(classes); });
    AdviserRegistry adviserRegistry = new AdviserRegistry();
    injector.injectMembers(adviserRegistry);
    classes.forEach(pair -> {
      Pair<AdviserType, Class<? extends Adviser>> adviserPair = (Pair<AdviserType, Class<? extends Adviser>>) pair;
      adviserRegistry.register(adviserPair.getLeft(), adviserPair.getRight());
    });
    return adviserRegistry;
  }

  @Provides
  @Singleton
  ResolverRegistry providesResolverRegistry(Injector injector, Map<String, ResolverRegistrar> resolverRegistrarMap) {
    Set classes = new HashSet<>();
    resolverRegistrarMap.values().forEach(resolverRegistrar -> { resolverRegistrar.register(classes); });
    ResolverRegistry resolverRegistry = new ResolverRegistry();
    injector.injectMembers(resolverRegistry);
    classes.forEach(pair -> {
      Pair<RefType, Class<? extends Resolver<?>>> statePair = (Pair<RefType, Class<? extends Resolver<?>>>) pair;
      resolverRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    return resolverRegistry;
  }

  @Provides
  @Singleton
  FacilitatorRegistry providesFacilitatorRegistry(
      Injector injector, Map<String, FacilitatorRegistrar> facilitatorRegistrarMap) {
    Set classes = new HashSet<>();
    facilitatorRegistrarMap.values().forEach(facilitatorRegistrar -> { facilitatorRegistrar.register(classes); });
    FacilitatorRegistry facilitatorRegistry = new FacilitatorRegistry();
    injector.injectMembers(facilitatorRegistry);
    classes.forEach(pair -> {
      Pair<FacilitatorType, Class<? extends Facilitator>> statePair =
          (Pair<FacilitatorType, Class<? extends Facilitator>>) pair;
      facilitatorRegistry.register(statePair.getLeft(), statePair.getRight());
    });
    return facilitatorRegistry;
  }

  @Provides
  @Singleton
  OrchestrationEventHandlerRegistry providesEventHandlerRegistry(
      Injector injector, Map<String, OrchestrationEventHandlerRegistrar> orchestrationEventHandlerRegistrarMap) {
    Set classes = new HashSet<>();
    orchestrationEventHandlerRegistrarMap.values().forEach(
        orchestrationEventHandlerRegistrar -> { orchestrationEventHandlerRegistrar.register(classes); });
    OrchestrationEventHandlerRegistry handlerRegistry = new OrchestrationEventHandlerRegistry();
    injector.injectMembers(handlerRegistry);
    classes.forEach(pair -> {
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
