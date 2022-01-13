/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exceptionhandler.handler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.SecretNotFoundRuntimeException;
import io.harness.exception.runtime.SecretRuntimeException;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class SecretExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(SecretRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    SecretRuntimeException ex = (SecretRuntimeException) exception;
    if (ex instanceof SecretNotFoundRuntimeException) {
      return new ExplanationException(
          String.format("Secret %s is not available under the scope of current %s.", ex.getSecretRef(), ex.getScope()),
          new HintException(
              String.format(
                  "Please verify %s actually exists in current %s scope. you can also create secret using below docs: https://ngdocs.harness.io/article/osfw70e59c-add-use-text-secrets",
                  ex.getSecretRef(), ex.getScope()),
              new io.harness.exception.SecretNotFoundException(ex.getMessage(), WingsException.USER)));
    } else {
      return new InvalidRequestException("Could not process secret properly");
    }
  }
}
