package io.harness.perpetualtask;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;

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

    bind(PerpetualTaskService.class).to(PerpetualTaskServiceImpl.class);
  }

  // This provider is for testing standalone Perpetual Task Service
  /*@Provides
  Datastore provideDatastore() {
    final Morphia morphia = new Morphia();
    return morphia.createDatastore(new MongoClient(), "harness");
  }*/
}