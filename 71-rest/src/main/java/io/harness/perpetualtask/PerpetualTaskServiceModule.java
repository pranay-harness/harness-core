package io.harness.perpetualtask;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;
import io.harness.perpetualtask.k8s.watch.K8sWatchServiceImpl;

public class PerpetualTaskServiceModule extends AbstractModule {
  private static volatile PerpetualTaskServiceModule instance;

  public static PerpetualTaskServiceModule getInstance() {
    if (instance == null) {
      instance = new PerpetualTaskServiceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder()
                .implement(PerpetualTaskService.class, PerpetualTaskServiceImpl.class)
                .build(PerpetualTaskServiceFactory.class));

    MapBinder<String, PerpetualTaskServiceClient> mapBinder =
        MapBinder.newMapBinder(binder(), String.class, PerpetualTaskServiceClient.class);
    mapBinder.addBinding(SamplePerpetualTaskServiceClient.class.getSimpleName())
        .to(SamplePerpetualTaskServiceClient.class);
    mapBinder.addBinding(K8sWatchServiceImpl.class.getSimpleName()).to(K8sWatchServiceImpl.class);

    bind(PerpetualTaskService.class).to(PerpetualTaskServiceImpl.class);
  }
}