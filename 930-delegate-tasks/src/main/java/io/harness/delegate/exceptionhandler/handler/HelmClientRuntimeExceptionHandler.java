/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exceptionhandler.handler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.DEFAULT_EXPLAIN_REPO_ADD;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.EXPLAIN_401_UNAUTHORIZED;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.EXPLAIN_403_FORBIDDEN;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.EXPLAIN_404_HELM_REPO;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.EXPLAIN_CHART_VERSION_IMPROPER_CONSTRAINT;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.EXPLAIN_MALFORMED_URL;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.EXPLAIN_MISSING_PROTOCOL_HANDLER;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.EXPLAIN_NO_CHART_FOUND;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Explanations.EXPLAIN_NO_CHART_VERSION_FOUND;
import static io.harness.delegate.task.helm.HelmExceptionConstants.HelmCliErrorMessages.CHART_VERSION_IMPROPER_CONSTRAINT;
import static io.harness.delegate.task.helm.HelmExceptionConstants.HelmCliErrorMessages.FORBIDDEN_403;
import static io.harness.delegate.task.helm.HelmExceptionConstants.HelmCliErrorMessages.NOT_FOUND_404;
import static io.harness.delegate.task.helm.HelmExceptionConstants.HelmCliErrorMessages.NO_CHART_FOUND;
import static io.harness.delegate.task.helm.HelmExceptionConstants.HelmCliErrorMessages.NO_CHART_VERSION_FOUND;
import static io.harness.delegate.task.helm.HelmExceptionConstants.HelmCliErrorMessages.NO_SUCH_HOST;
import static io.harness.delegate.task.helm.HelmExceptionConstants.HelmCliErrorMessages.PROTOCOL_HANDLER_MISSING;
import static io.harness.delegate.task.helm.HelmExceptionConstants.HelmCliErrorMessages.UNAUTHORIZED_401;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.DEFAULT_HINT_REPO_ADD;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.HINT_401_UNAUTHORIZED;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.HINT_403_FORBIDDEN;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.HINT_404_HELM_REPO;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.HINT_CHART_VERSION_IMPROPER_CONSTRAINT;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.HINT_MALFORMED_URL;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.HINT_MISSING_PROTOCOL_HANDLER;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.HINT_NO_CHART_FOUND;
import static io.harness.delegate.task.helm.HelmExceptionConstants.Hints.HINT_NO_CHART_VERSION_FOUND;

import static com.amazonaws.util.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.HelmClientException;
import io.harness.exception.HelmClientRuntimeException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.helm.HelmCliCommandType;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Set;

@OwnedBy(CDP)
@Singleton
public class HelmClientRuntimeExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(HelmClientRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    final HelmClientRuntimeException helmClientRuntimeException = (HelmClientRuntimeException) exception;
    final HelmClientException cause = helmClientRuntimeException.getHelmClientException();
    if (isRepoAddCommand(cause)) {
      return handleRepoAddException(cause);
    }

    if (isFetchCommand(cause)) {
      return handleFetchException(cause);
    }

    return new InvalidRequestException(defaultIfEmpty(cause.getMessage(), ""));
  }

  private WingsException handleRepoAddException(HelmClientException helmClientException) {
    final String message = helmClientException.getMessage();
    final String lowerCaseMessage = lowerCase(helmClientException.getMessage());
    if (lowerCaseMessage.contains(NOT_FOUND_404)) {
      return NestedExceptionUtils.hintWithExplanationException(
          HINT_404_HELM_REPO, EXPLAIN_404_HELM_REPO, new InvalidRequestException(message));
    } else if (lowerCaseMessage.contains(UNAUTHORIZED_401)) {
      return NestedExceptionUtils.hintWithExplanationException(
          HINT_401_UNAUTHORIZED, EXPLAIN_401_UNAUTHORIZED, new InvalidRequestException(message));
    } else if (lowerCaseMessage.contains(FORBIDDEN_403)) {
      return NestedExceptionUtils.hintWithExplanationException(
          HINT_403_FORBIDDEN, EXPLAIN_403_FORBIDDEN, new InvalidRequestException(message));
    } else if (lowerCaseMessage.contains(NO_SUCH_HOST)) {
      return NestedExceptionUtils.hintWithExplanationException(
          HINT_MALFORMED_URL, EXPLAIN_MALFORMED_URL, new InvalidRequestException(message));
    } else if (lowerCaseMessage.contains(PROTOCOL_HANDLER_MISSING)) {
      return NestedExceptionUtils.hintWithExplanationException(
          HINT_MISSING_PROTOCOL_HANDLER, EXPLAIN_MISSING_PROTOCOL_HANDLER, new InvalidRequestException(message));
    }
    return NestedExceptionUtils.hintWithExplanationException(
        DEFAULT_HINT_REPO_ADD, DEFAULT_EXPLAIN_REPO_ADD, new InvalidRequestException(message));
  }

  private WingsException handleFetchException(HelmClientException helmClientException) {
    final String lowerCaseMessage = lowerCase(helmClientException.getMessage());

    if (lowerCaseMessage.contains(NO_CHART_FOUND)) {
      return NestedExceptionUtils.hintWithExplanationException(
          HINT_NO_CHART_FOUND, EXPLAIN_NO_CHART_FOUND, helmClientException);
    } else if (lowerCaseMessage.contains(NO_CHART_VERSION_FOUND)) {
      return NestedExceptionUtils.hintWithExplanationException(
          HINT_NO_CHART_VERSION_FOUND, EXPLAIN_NO_CHART_VERSION_FOUND, helmClientException);
    } else if (lowerCaseMessage.contains(CHART_VERSION_IMPROPER_CONSTRAINT)) {
      return NestedExceptionUtils.hintWithExplanationException(
          HINT_CHART_VERSION_IMPROPER_CONSTRAINT, EXPLAIN_CHART_VERSION_IMPROPER_CONSTRAINT, helmClientException);
    }

    return helmClientException;
  }

  private boolean isRepoAddCommand(HelmClientException helmClientException) {
    return helmClientException.getHelmCliCommandType() == HelmCliCommandType.REPO_ADD;
  }

  private boolean isFetchCommand(HelmClientException helmClientException) {
    return helmClientException.getHelmCliCommandType() == HelmCliCommandType.FETCH;
  }
}
