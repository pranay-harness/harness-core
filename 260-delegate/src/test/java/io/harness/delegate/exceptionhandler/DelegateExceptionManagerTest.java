package io.harness.delegate.exceptionhandler;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.DelegateTestBase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.exception.DelegateErrorHandlerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.KryoHandlerNotFoundException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class DelegateExceptionManagerTest extends DelegateTestBase {
  @Inject DelegateExceptionManager delegateExceptionManager;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testBasicSanityCaseForSingleException() {
    AmazonCodeDeployException amazonServiceException = new AmazonCodeDeployException("Amazon Code deploy exception");
    DelegateResponseData delegateResponseData =
        delegateExceptionManager.getResponseData(amazonServiceException, null, true);
    assertThat(delegateResponseData).isNotNull();

    ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
    assertThat(errorNotifyResponseData.getException()).isNotNull();
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testUnhandledException() {
    RuntimeException exception = new RuntimeException("Unknown Runtime exception");
    DelegateResponseData delegateResponseData = delegateExceptionManager.getResponseData(exception, null, true);
    assertThat(delegateResponseData).isNotNull();

    ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
    assertThat(errorNotifyResponseData.getException()).isNotNull();
    assertThat(errorNotifyResponseData.getException() instanceof DelegateErrorHandlerException).isTrue();
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testExceptionWhenFrameworkUnsupported() {
    RuntimeException exception = new RuntimeException("Unknown Runtime exception");
    DelegateResponseData delegateResponseData = delegateExceptionManager.getResponseData(exception, null, false);
    assertThat(delegateResponseData).isNotNull();

    ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
    assertThat(errorNotifyResponseData.getException()).isNull();
    assertThat(errorNotifyResponseData.getErrorMessage().equals(ExceptionUtils.getMessage(exception))).isTrue();
    assertThat(errorNotifyResponseData.getFailureTypes().equals(ExceptionUtils.getFailureTypes(exception))).isTrue();
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testCascadedException() {
    AmazonCodeDeployException amazonServiceException = new AmazonCodeDeployException("Amazon Code deploy exception");
    InvalidArtifactServerException invalidArtifactServerException =
        new InvalidArtifactServerException("Invalid Artifact Server", amazonServiceException);

    DelegateResponseData delegateResponseData =
        delegateExceptionManager.getResponseData(invalidArtifactServerException, null, true);
    assertThat(delegateResponseData).isNotNull();

    ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
    assertThat(errorNotifyResponseData.getException()).isNotNull();

    WingsException exception = errorNotifyResponseData.getException();
    assertThat(exception instanceof InvalidArtifactServerException).isTrue();
    Exception cause = (Exception) exception.getCause();
    assertThat(cause instanceof InvalidRequestException).isTrue();
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testUnhandledExceptionNotRegisteredWithKryo() {
    String errorMessage = "Random exception not registered to Kryo";
    RandomRuntimeException randomRuntimeException = new RandomRuntimeException(errorMessage);

    DelegateResponseData delegateResponseData =
        delegateExceptionManager.getResponseData(randomRuntimeException, null, true);
    assertThat(delegateResponseData).isNotNull();

    ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
    assertThat(errorNotifyResponseData.getException()).isNotNull();

    WingsException exception = errorNotifyResponseData.getException();
    assertThat(exception instanceof DelegateErrorHandlerException).isTrue();
    Exception cause = (Exception) exception.getCause();
    assertThat(cause instanceof KryoHandlerNotFoundException).isTrue();
    assertThat(cause.getMessage().equals(errorMessage)).isTrue();
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testThrowable() {
    Throwable throwable = new Throwable("Throwable  exception");
    DelegateResponseData delegateResponseData = delegateExceptionManager.getResponseData(throwable, null, false);
    assertThat(delegateResponseData).isNotNull();

    ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
    assertThat(errorNotifyResponseData.getException()).isNull();
    assertThat(errorNotifyResponseData.getErrorMessage().equals(ExceptionUtils.getMessage(throwable))).isTrue();
    assertThat(errorNotifyResponseData.getFailureTypes().equals(ExceptionUtils.getFailureTypes(throwable))).isTrue();
  }

  public static class RandomRuntimeException extends RuntimeException {
    public RandomRuntimeException(String message) {
      super(message);
    }
  }
}