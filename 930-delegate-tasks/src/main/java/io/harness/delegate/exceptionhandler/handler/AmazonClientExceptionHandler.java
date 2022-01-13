/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exceptionhandler.handler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.amazonaws.AmazonClientException;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DX)
@Singleton
public class AmazonClientExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(AmazonClientException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    AmazonClientException amazonClientException = (AmazonClientException) exception;
    log.error("AWS API Client call exception: {}", amazonClientException.getMessage());
    String errorMessage = amazonClientException.getMessage();
    if (isNotEmpty(errorMessage) && errorMessage.contains("/meta-data/iam/security-credentials/")) {
      return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_AWS_IAM_ROLE_CHECK,
          ExplanationException.EXPLANATION_AWS_AM_ROLE_CHECK,
          new InvalidRequestException("The IAM role on the Ec2 delegate does not exist OR does not"
                  + " have required permissions. :: " + errorMessage,
              USER));
    } else {
      return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_AWS_CLIENT_UNKNOWN_ISSUE,
          ExplanationException.EXPLANATION_AWS_AM_ROLE_CHECK,
          new InvalidRequestException(isNotEmpty(errorMessage) ? errorMessage : "Unknown Aws client exception", USER));
    }
  }
}
