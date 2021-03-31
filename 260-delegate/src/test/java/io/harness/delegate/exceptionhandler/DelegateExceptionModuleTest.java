package io.harness.delegate.exceptionhandler;

import io.harness.delegate.exceptionhandler.handler.AmazonClientExceptionHandler;
import io.harness.delegate.exceptionhandler.handler.AmazonServiceExceptionHandler;
import io.harness.exception.exceptionmanager.ExceptionHandler;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

public class DelegateExceptionModuleTest extends AbstractModule {
  private static volatile DelegateExceptionModuleTest instance;

  public static DelegateExceptionModuleTest getInstance() {
    if (instance == null) {
      instance = new DelegateExceptionModuleTest();
    }
    return instance;
  }

  @Override
  protected void configure() {
    MapBinder<Class<? extends Exception>, ExceptionHandler> exceptionHandlerMapBinder = MapBinder.newMapBinder(
        binder(), new TypeLiteral<Class<? extends Exception>>() {}, new TypeLiteral<ExceptionHandler>() {});

    exceptionHandlerMapBinder.addBinding(AmazonServiceException.class).to(AmazonServiceExceptionHandler.class);
    exceptionHandlerMapBinder.addBinding(AmazonClientException.class).to(AmazonClientExceptionHandler.class);
  }
}
