package io.harness;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.harness.registrars.NGPipelineCommonsFieldRegistrar;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;

public class NGPipelineCommonsModule extends AbstractModule {
  private static NGPipelineCommonsModule instance;

  public static NGPipelineCommonsModule getInstance() {
    if (instance == null) {
      instance = new NGPipelineCommonsModule();
    }
    return instance;
  }

  private NGPipelineCommonsModule() {}

  @Override
  protected void configure() {
    install(OrchestrationModule.getInstance());

    MapBinder<String, OrchestrationFieldRegistrar> orchestrationFieldRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationFieldRegistrar.class);
    orchestrationFieldRegistrarMapBinder.addBinding(NGPipelineCommonsFieldRegistrar.class.getName())
        .to(NGPipelineCommonsFieldRegistrar.class);
  }
}
