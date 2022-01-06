/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exceptionhandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.exceptionhandler.handler.AmazonClientExceptionHandler;
import io.harness.delegate.exceptionhandler.handler.AmazonServiceExceptionHandler;
import io.harness.exception.DelegateServiceDriverExceptionHandler;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import io.grpc.StatusRuntimeException;

@OwnedBy(HarnessTeam.DX)
public class TestingExceptionModule extends AbstractModule {
  private static volatile TestingExceptionModule instance;

  public static TestingExceptionModule getInstance() {
    if (instance == null) {
      instance = new TestingExceptionModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    MapBinder<Class<? extends Exception>, ExceptionHandler> exceptionHandlerMapBinder = MapBinder.newMapBinder(
        binder(), new TypeLiteral<Class<? extends Exception>>() {}, new TypeLiteral<ExceptionHandler>() {});

    exceptionHandlerMapBinder.addBinding(AmazonServiceException.class).to(AmazonServiceExceptionHandler.class);
    exceptionHandlerMapBinder.addBinding(AmazonClientException.class).to(AmazonClientExceptionHandler.class);

    exceptionHandlerMapBinder.addBinding(StatusRuntimeException.class).to(DelegateServiceDriverExceptionHandler.class);
  }
}
