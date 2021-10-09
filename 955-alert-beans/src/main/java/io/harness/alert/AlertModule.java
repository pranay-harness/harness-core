package io.harness.alert;

import com.google.inject.AbstractModule;

public class AlertModule extends AbstractModule {
  private static volatile AlertModule instance;

  public static AlertModule getInstance() {
    if (instance == null) {
      instance = new AlertModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bindAlerts();
  }

  private void bindAlerts() {
    //    MapBinder<AlertType, Class<? extends AlertData>> mapBinder = MapBinder.newMapBinder(
    //        binder(), new TypeLiteral<AlertType>() {}, new TypeLiteral<Class<? extends AlertData>>() {});
  }
}
