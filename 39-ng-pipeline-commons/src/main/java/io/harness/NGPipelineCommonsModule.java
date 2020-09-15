package io.harness;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.harness.registrars.NGPipelineCommonsFieldRegistrar;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;

public class NGPipelineCommonsModule extends AbstractModule {
  private static NGPipelineCommonsModule instance;

  public static NGPipelineCommonsModule getInstance(OrchestrationModuleConfig config) {
    if (instance == null) {
      instance = new NGPipelineCommonsModule(config);
    }
    return instance;
  }

  private final OrchestrationModuleConfig config;

  private NGPipelineCommonsModule(OrchestrationModuleConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(OrchestrationModule.getInstance(config));

    MapBinder<String, OrchestrationFieldRegistrar> orchestrationFieldRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationFieldRegistrar.class);
    orchestrationFieldRegistrarMapBinder.addBinding(NGPipelineCommonsFieldRegistrar.class.getName())
        .to(NGPipelineCommonsFieldRegistrar.class);
  }
}
