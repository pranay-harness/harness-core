package io.harness.delegate.exceptionhandler;

import io.harness.delegate.exceptionhandler.handler.AmazonServiceExceptionHandler;
import io.harness.delegate.exceptionhandler.handler.DelegateExceptionHandler;

import com.amazonaws.AmazonServiceException;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

public class DelegateExceptionModule extends AbstractModule {
  private static volatile DelegateExceptionModule instance;

  public static DelegateExceptionModule getInstance() {
    if (instance == null) {
      instance = new DelegateExceptionModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    MapBinder<Class<? extends Exception>, DelegateExceptionHandler> delegateExceptionHandlerMapBinder =
        MapBinder.newMapBinder(
            binder(), new TypeLiteral<Class<? extends Exception>>() {}, new TypeLiteral<DelegateExceptionHandler>() {});

    delegateExceptionHandlerMapBinder.addBinding(AmazonServiceException.class).to(AmazonServiceExceptionHandler.class);
  }
}
