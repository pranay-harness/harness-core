package io.harness.ng;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.harness.OrchestrationModule;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;
import io.harness.serializer.registrars.NGPipelineCommonsFieldRegistrar;

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
    orchestrationFieldRegistrarMapBinder.addBinding(NGPipelineCommonsFieldRegistrar.class.getName())
        .to(NGPipelineCommonsFieldRegistrar.class);
  }
}
