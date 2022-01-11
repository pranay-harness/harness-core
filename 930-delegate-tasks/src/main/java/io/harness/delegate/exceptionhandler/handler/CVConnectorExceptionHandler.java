/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exceptionhandler.handler;

import static io.harness.exception.HintException.HINT_CHECK_AUTHORIZATION_DETAILS;
import static io.harness.exception.HintException.HINT_CHECK_URL_DETAILS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.datacollection.exception.DataCollectionException;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CV)
@Singleton
public class CVConnectorExceptionHandler implements ExceptionHandler {
  public static String URL_NOT_FOUND = "URL not found";
  public static String USER_NOT_AUTHORIZED = "User not authorized";

  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(DataCollectionException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    DataCollectionException ex = (DataCollectionException) exception;
    if (ex.hasStatusCode()) {
      if (ex.getStatusCode().intValue() == 404) {
        return new ExplanationException(String.format(ExplanationException.URL_NOT_FOUND),
            new HintException(HINT_CHECK_URL_DETAILS, new InvalidRequestException(URL_NOT_FOUND)));
      } else if (ex.getStatusCode().intValue() == 401 || ex.getStatusCode().intValue() == 403) {
        return new ExplanationException(String.format(ExplanationException.AUTHORIZATION_FAILURE),
            new HintException(HINT_CHECK_AUTHORIZATION_DETAILS, new InvalidRequestException(USER_NOT_AUTHORIZED)));
      } else {
        return new InvalidRequestException(exception.getMessage());
      }
    } else {
      return new InvalidRequestException(exception.getMessage());
    }
  }
}
