package io.harness.pms.sdk.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.expression.OrchestrationFieldProcessor;
import io.harness.pms.expression.OrchestrationFieldType;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.registries.*;
import io.harness.pms.sdk.core.resolver.Resolver;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.registries.registrar.*;
import io.harness.pms.sdk.registries.registrar.local.PmsSdkAdviserRegistrar;
import io.harness.pms.sdk.registries.registrar.local.PmsSdkFacilitatorRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
@Slf4j
public class PmsSdkRegistryModule extends AbstractModule {
  private final PmsSdkConfiguration config;

  private static PmsSdkRegistryModule instance;

  public static synchronized PmsSdkRegistryModule getInstance(PmsSdkConfiguration config) {
    if (instance == null) {
      instance = new PmsSdkRegistryModule(config);
    }
    return instance;
  }

  public PmsSdkRegistryModule(PmsSdkConfiguration config) {
    this.config = config;
  }

  public void configure() {
    MapBinder<String, FacilitatorRegistrar> facilitatorRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, FacilitatorRegistrar.class);
    facilitatorRegistrarMapBinder.addBinding(PmsSdkFacilitatorRegistrar.class.getName())
        .to(PmsSdkFacilitatorRegistrar.class);

    MapBinder<String, AdviserRegistrar> adviserRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, AdviserRegistrar.class);
    adviserRegistrarMapBinder.addBinding(PmsSdkAdviserRegistrar.class.getName()).to(PmsSdkAdviserRegistrar.class);

    MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);

    MapBinder.newMapBinder(binder(), String.class, ResolverRegistrar.class);

    MapBinder.newMapBinder(binder(), String.class, OrchestrationFieldRegistrar.class);
  }

  @Provides
  @Singleton
  StepRegistry providesStateRegistry() {
    StepRegistry stepRegistry = new StepRegistry();
    Map<StepType, Step> engineSteps = config.getEngineSteps();
    if (EmptyPredicate.isNotEmpty(engineSteps)) {
      engineSteps.forEach(stepRegistry::register);
    }
    return stepRegistry;
  }

  @Provides
  @Singleton
  AdviserRegistry providesAdviserRegistry(Injector injector, Map<String, AdviserRegistrar> adviserRegistrarMap) {
    Set<Pair<AdviserType, Adviser>> classes = new HashSet<>();
    adviserRegistrarMap.values().forEach(adviserRegistrar -> adviserRegistrar.register(classes));
    AdviserRegistry adviserRegistry = new AdviserRegistry();
    injector.injectMembers(adviserRegistry);
    classes.forEach(pair -> adviserRegistry.register(pair.getLeft(), pair.getRight()));
    return adviserRegistry;
  }

  @Provides
  @Singleton
  ResolverRegistry providesResolverRegistry(Injector injector, Map<String, ResolverRegistrar> resolverRegistrarMap) {
    Set<Pair<RefType, Resolver<?>>> classes = new HashSet<>();
    resolverRegistrarMap.values().forEach(resolverRegistrar -> resolverRegistrar.register(classes));
    ResolverRegistry resolverRegistry = new ResolverRegistry();
    injector.injectMembers(resolverRegistry);
    classes.forEach(pair -> { resolverRegistry.register(pair.getLeft(), pair.getRight()); });
    return resolverRegistry;
  }

  @Provides
  @Singleton
  FacilitatorRegistry providesFacilitatorRegistry(
      Injector injector, Map<String, FacilitatorRegistrar> facilitatorRegistrarMap) {
    Set<Pair<FacilitatorType, Facilitator>> pairs = new HashSet<>();
    facilitatorRegistrarMap.values().forEach(facilitatorRegistrar -> facilitatorRegistrar.register(pairs));
    FacilitatorRegistry facilitatorRegistry = new FacilitatorRegistry();
    injector.injectMembers(facilitatorRegistry);
    pairs.forEach(pair -> { facilitatorRegistry.register(pair.getLeft(), pair.getRight()); });
    return facilitatorRegistry;
  }

  @Provides
  @Singleton
  OrchestrationEventHandlerRegistry providesEventHandlerRegistry(
      Injector injector, Map<String, OrchestrationEventHandlerRegistrar> orchestrationEventHandlerRegistrarMap) {
    Set<Pair<OrchestrationEventType, OrchestrationEventHandler>> classes = new HashSet<>();
    orchestrationEventHandlerRegistrarMap.values().forEach(
        orchestrationEventHandlerRegistrar -> orchestrationEventHandlerRegistrar.register(classes));
    OrchestrationEventHandlerRegistry handlerRegistry = new OrchestrationEventHandlerRegistry();
    injector.injectMembers(handlerRegistry);
    classes.forEach(pair -> handlerRegistry.register(pair.getLeft(), Collections.singleton(pair.getRight())));
    return handlerRegistry;
  }

  @Provides
  @Singleton
  OrchestrationFieldRegistry providesOrchestrationFieldRegistry(
      Injector injector, Map<String, OrchestrationFieldRegistrar> orchestrationFieldRegistrarMap) {
    Set<Pair<OrchestrationFieldType, OrchestrationFieldProcessor>> classes = new HashSet<>();
    orchestrationFieldRegistrarMap.values().forEach(
        orchestrationFieldRegistrar -> orchestrationFieldRegistrar.register(classes));
    OrchestrationFieldRegistry orchestrationFieldRegistry = new OrchestrationFieldRegistry();
    injector.injectMembers(orchestrationFieldRegistry);
    classes.forEach(pair -> orchestrationFieldRegistry.register(pair.getLeft(), pair.getRight()));
    return orchestrationFieldRegistry;
  }
}
