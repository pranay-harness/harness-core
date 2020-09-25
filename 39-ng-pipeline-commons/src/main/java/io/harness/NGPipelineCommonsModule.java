package io.harness;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.harness.registrars.NGPipelineOrchestrationFieldRegistrar;
import io.harness.registrars.NGPipelineVisitorFieldRegistrar;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;
import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;

import java.util.concurrent.atomic.AtomicReference;

public class NGPipelineCommonsModule extends AbstractModule {
  private static final AtomicReference<NGPipelineCommonsModule> instanceRef = new AtomicReference<>();

  public static NGPipelineCommonsModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGPipelineCommonsModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    install(OrchestrationModule.getInstance());

    MapBinder<String, OrchestrationFieldRegistrar> orchestrationFieldRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationFieldRegistrar.class);
    orchestrationFieldRegistrarMapBinder.addBinding(NGPipelineOrchestrationFieldRegistrar.class.getName())
        .to(NGPipelineOrchestrationFieldRegistrar.class);

    MapBinder<String, VisitableFieldRegistrar> visitableFieldRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, VisitableFieldRegistrar.class);
    visitableFieldRegistrarMapBinder.addBinding(NGPipelineVisitorFieldRegistrar.class.getName())
        .to(NGPipelineVisitorFieldRegistrar.class);
  }
}
