package io.harness.delegate.exceptionhandler.handler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.exceptionhandler.handler.KubernetesApiExceptionHandler.API_CALL_FAIL_MESSAGE;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.k8s.exception.KubernetesExceptionExplanation;
import io.harness.delegate.k8s.exception.KubernetesExceptionHints;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.KubernetesTaskException;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class KubernetesApiExceptionHandlerTest extends CategoryTest {
  KubernetesApiExceptionHandler apiExceptionHandler = new KubernetesApiExceptionHandler();

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApiIOException() {
    String ioMessage = "connectivity issue";
    assertThat(apiExceptionHandler.handleException(new ApiException(new IOException(ioMessage))))
        .isInstanceOf(HintException.class)
        .hasMessage(KubernetesExceptionHints.K8S_API_GENERIC_NETWORK_EXCEPTION)
        .getCause()
        .isInstanceOf(ExplanationException.class)
        .hasMessage(KubernetesExceptionExplanation.K8S_API_IO_EXCEPTION)
        .getCause()
        .isInstanceOf(KubernetesTaskException.class)
        .hasMessage(ioMessage);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApiSocketTimeout() {
    String timeoutMessage = "timeout issue";
    assertThat(apiExceptionHandler.handleException(new ApiException(new SocketTimeoutException(timeoutMessage))))
        .isInstanceOf(HintException.class)
        .hasMessage(KubernetesExceptionHints.K8S_API_SOCKET_TIMEOUT_EXCEPTION)
        .getCause()
        .isInstanceOf(ExplanationException.class)
        .hasMessage(KubernetesExceptionExplanation.K8S_API_SOCKET_TIMEOUT_EXCEPTION)
        .getCause()
        .isInstanceOf(KubernetesTaskException.class)
        .hasMessage(timeoutMessage);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApiOtherConnectivityIssue() {
    String message = "some network issue";
    assertThat(apiExceptionHandler.handleException(new ApiException(new RuntimeException(message))))
        .isInstanceOf(HintException.class)
        .hasMessage(KubernetesExceptionHints.K8S_API_GENERIC_NETWORK_EXCEPTION)
        .getCause()
        .isInstanceOf(ExplanationException.class)
        .hasMessage(KubernetesExceptionExplanation.K8S_API_GENERIC_NETWORK_EXCEPTION)
        .getCause()
        .isInstanceOf(KubernetesTaskException.class)
        .hasMessage(message);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApiForbidden() {
    String responseBody = "body";
    assertThat(apiExceptionHandler.handleException(new ApiException(403, ImmutableMap.of(), responseBody)))
        .isInstanceOf(HintException.class)
        .hasMessage(KubernetesExceptionHints.K8S_API_FORBIDDEN_EXCEPTION)
        .getCause()
        .isInstanceOf(ExplanationException.class)
        .hasMessage(KubernetesExceptionExplanation.K8S_API_FORBIDDEN_EXCEPTION)
        .getCause()
        .isInstanceOf(KubernetesTaskException.class)
        .hasMessage(API_CALL_FAIL_MESSAGE + responseBody);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApiUnauthorized() {
    String responseBody = "body";
    assertThat(apiExceptionHandler.handleException(new ApiException(401, ImmutableMap.of(), responseBody)))
        .isInstanceOf(HintException.class)
        .hasMessage(KubernetesExceptionHints.K8S_API_UNAUTHORIZED_EXCEPTION)
        .getCause()
        .isInstanceOf(ExplanationException.class)
        .hasMessage(KubernetesExceptionExplanation.K8S_API_UNAUTHORIZED_EXCEPTION)
        .getCause()
        .isInstanceOf(KubernetesTaskException.class)
        .hasMessage(API_CALL_FAIL_MESSAGE + responseBody);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApiOtherHttpError() {
    assertThat(apiExceptionHandler.handleException(new ApiException(309, ImmutableMap.of(), "body")))
        .isInstanceOf(ExplanationException.class)
        .hasMessage("Failed to perform Kubernetes API call. Response HTTP code: 309. Response HTTP Body: body");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApiValidationError() {
    String validationMessage = "param null";
    assertThat(apiExceptionHandler.handleException(new ApiException(validationMessage)))
        .isInstanceOf(HintException.class)
        .hasMessage(KubernetesExceptionHints.K8S_API_VALIDATION_ERROR)
        .getCause()
        .isInstanceOf(ExplanationException.class)
        .hasMessage(KubernetesExceptionExplanation.K8S_API_VALIDATION_ERROR)
        .getCause()
        .isInstanceOf(KubernetesTaskException.class)
        .hasMessage(validationMessage);
  }
}