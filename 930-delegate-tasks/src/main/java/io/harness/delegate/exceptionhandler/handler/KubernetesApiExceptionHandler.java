package io.harness.delegate.exceptionhandler.handler;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.k8s.exception.KubernetesExceptionExplanation;
import io.harness.delegate.k8s.exception.KubernetesExceptionHints;
import io.harness.exception.ExplanationException;
import io.harness.exception.FailureType;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.google.common.collect.ImmutableSet;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;

@OwnedBy(CDP)
public class KubernetesApiExceptionHandler implements ExceptionHandler {
  public static String API_CALL_FAIL_MESSAGE = "Failed with response: ";

  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.of(ApiException.class);
  }

  @Override
  public WingsException handleException(Exception exception) {
    ApiException apiException = (ApiException) exception;

    if (apiException.getCause() != null) {
      if (apiException.getCause() instanceof SocketTimeoutException) {
        return NestedExceptionUtils.hintWithExplanationException(
            KubernetesExceptionHints.K8S_API_SOCKET_TIMEOUT_EXCEPTION,
            KubernetesExceptionExplanation.K8S_API_SOCKET_TIMEOUT_EXCEPTION,
            new KubernetesTaskException(apiException.getCause().getMessage(), FailureType.TIMEOUT_ERROR));
      } else if (apiException.getCause() instanceof IOException) {
        return NestedExceptionUtils.hintWithExplanationException(
            KubernetesExceptionHints.K8S_API_GENERIC_NETWORK_EXCEPTION,
            KubernetesExceptionExplanation.K8S_API_IO_EXCEPTION,
            new KubernetesTaskException(apiException.getCause().getMessage(), FailureType.CONNECTIVITY));
      } else {
        return NestedExceptionUtils.hintWithExplanationException(
            KubernetesExceptionHints.K8S_API_GENERIC_NETWORK_EXCEPTION,
            KubernetesExceptionExplanation.K8S_API_GENERIC_NETWORK_EXCEPTION,
            new KubernetesTaskException(apiException.getCause().getMessage(), FailureType.CONNECTIVITY));
      }
    } else if (apiException.getCode() > 0) {
      switch (apiException.getCode()) {
        case 403:
          return NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.K8S_API_FORBIDDEN_EXCEPTION,
              KubernetesExceptionExplanation.K8S_API_FORBIDDEN_EXCEPTION,
              new KubernetesTaskException(
                  API_CALL_FAIL_MESSAGE + apiException.getResponseBody(), FailureType.AUTHORIZATION_ERROR));

        case 401:
          return NestedExceptionUtils.hintWithExplanationException(
              KubernetesExceptionHints.K8S_API_UNAUTHORIZED_EXCEPTION,
              KubernetesExceptionExplanation.K8S_API_UNAUTHORIZED_EXCEPTION,
              new KubernetesTaskException(
                  API_CALL_FAIL_MESSAGE + apiException.getResponseBody(), FailureType.AUTHENTICATION));

        default:
          return new ExplanationException(
              format("Failed to perform Kubernetes API call. Response HTTP code: %d. Response HTTP Body: %s",
                  apiException.getCode(), apiException.getResponseBody()),
              null);
      }

    } else {
      // Missing cause or code & response body in all known cases is related to validation exception
      return NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.K8S_API_VALIDATION_ERROR,
          KubernetesExceptionExplanation.K8S_API_VALIDATION_ERROR,
          new KubernetesTaskException(apiException.getMessage(), FailureType.APPLICATION_ERROR));
    }
  }
}
