package io.harness.exception.exceptionmanager;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

@OwnedBy(HarnessTeam.DX)
public class ExceptionModule extends AbstractModule {
  private static volatile ExceptionModule instance;

  public static ExceptionModule getInstance() {
    if (instance == null) {
      instance = new ExceptionModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    MapBinder<Class<? extends Exception>, ExceptionHandler> exceptionHandlerMapBinder = MapBinder.newMapBinder(
        binder(), new TypeLiteral<Class<? extends Exception>>() {}, new TypeLiteral<ExceptionHandler>() {});
  }
}
