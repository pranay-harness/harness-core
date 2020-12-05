package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.OrchestrationFieldType;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.refobjects.RefType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.steps.StepType;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.events.OrchestrationEventHandlerRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.field.OrchestrationFieldRegistry;
import io.harness.registries.registrar.AdviserRegistrar;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StepRegistry;
import io.harness.resolvers.Resolver;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
@Slf4j
public class OrchestrationRegistryModule extends AbstractModule {
  private static OrchestrationRegistryModule instance;

  public static synchronized OrchestrationRegistryModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationRegistryModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  StepRegistry providesStateRegistry(Injector injector, Map<String, StepRegistrar> stepRegistrarMap) {
    Set<Pair<StepType, Step>> classes = new HashSet<>();
    stepRegistrarMap.values().forEach(stepRegistrar -> stepRegistrar.register(classes));
    StepRegistry stepRegistry = new StepRegistry();
    injector.injectMembers(stepRegistry);
    classes.forEach(pair -> { stepRegistry.register(pair.getLeft(), pair.getRight()); });
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
