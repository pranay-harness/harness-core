package io.harness.delegate.task.k8s.exception;

import static java.lang.String.format;

import io.harness.exception.FailureType;
import io.harness.exception.KubernetesApiTaskException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;

@Singleton
public class KubernetesApiExceptionHandler implements ExceptionHandler {
  public static String API_CALL_FAIL_MESSAGE = "Kubernetes API call failed with message: %s";

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
            new KubernetesApiTaskException(apiException.getCause().getMessage(), FailureType.TIMEOUT_ERROR));
      } else if (apiException.getCause() instanceof IOException) {
        return NestedExceptionUtils.hintWithExplanationException(
            KubernetesExceptionHints.K8S_API_GENERIC_NETWORK_EXCEPTION,
            KubernetesExceptionExplanation.K8S_API_IO_EXCEPTION,
            new KubernetesApiTaskException(apiException.getCause().getMessage(), FailureType.CONNECTIVITY));
      } else {
        return NestedExceptionUtils.hintWithExplanationException(
            KubernetesExceptionHints.K8S_API_GENERIC_NETWORK_EXCEPTION,
            KubernetesExceptionExplanation.K8S_API_GENERIC_NETWORK_EXCEPTION,
            new KubernetesApiTaskException(apiException.getCause().getMessage(), FailureType.CONNECTIVITY));
      }
    } else if (apiException.getCode() > 0) {
      switch (apiException.getCode()) {
        case 403:
          return NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.K8S_API_FORBIDDEN_EXCEPTION,
              KubernetesExceptionExplanation.K8S_API_FORBIDDEN_EXCEPTION,
              new KubernetesApiTaskException(
                  format(API_CALL_FAIL_MESSAGE, apiException.getMessage()), FailureType.AUTHORIZATION_ERROR));

        case 401:
          return NestedExceptionUtils.hintWithExplanationException(
              KubernetesExceptionHints.K8S_API_UNAUTHORIZED_EXCEPTION,
              KubernetesExceptionExplanation.K8S_API_UNAUTHORIZED_EXCEPTION,
              new KubernetesApiTaskException(
                  format(API_CALL_FAIL_MESSAGE, apiException.getMessage()), FailureType.AUTHENTICATION));

        default:
          return new KubernetesApiTaskException(format(API_CALL_FAIL_MESSAGE, apiException.getMessage()));
      }

    } else {
      // Missing cause or code & response body in all known cases is related to validation exception
      return NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.K8S_API_VALIDATION_ERROR,
          KubernetesExceptionExplanation.K8S_API_VALIDATION_ERROR,
          new KubernetesApiTaskException(apiException.getMessage()));
    }
  }
}
