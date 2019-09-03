package io.harness.perpetualtask;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

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
    MapBinder<PerpetualTaskType, PerpetualTaskServiceClient> mapBinder =
        MapBinder.newMapBinder(binder(), PerpetualTaskType.class, PerpetualTaskServiceClient.class);
    for (PerpetualTaskType perpetualTaskType : PerpetualTaskType.values()) {
      mapBinder.addBinding(perpetualTaskType)
          .to(perpetualTaskType.getPerpetualTaskServiceClientClass())
          .in(Singleton.class);
    }

    bind(PerpetualTaskService.class).to(PerpetualTaskServiceImpl.class);
  }
}