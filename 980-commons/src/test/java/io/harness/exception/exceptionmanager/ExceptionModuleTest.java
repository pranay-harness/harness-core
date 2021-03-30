package io.harness.exception.exceptionmanager;

import com.amazonaws.AmazonServiceException;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

public class ExceptionModuleTest extends AbstractModule {
  private static volatile ExceptionModuleTest instance;

  public static ExceptionModuleTest getInstance() {
    if (instance == null) {
      instance = new ExceptionModuleTest();
    }
    return instance;
  }

  @Override
  protected void configure() {
    MapBinder<Class<? extends Exception>, ExceptionHandler> exceptionHandlerMapBinder = MapBinder.newMapBinder(
        binder(), new TypeLiteral<Class<? extends Exception>>() {}, new TypeLiteral<ExceptionHandler>() {});

    exceptionHandlerMapBinder.addBinding(AmazonServiceException.class).to(AmazonServiceExceptionHandler.class);
  }
}
