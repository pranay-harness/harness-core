package io.harness.exception.exceptionmanager;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.DX)
public class ExceptionManagerTest extends CategoryTest {
  @Inject ExceptionManager exceptionManager;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testBasicSanityCaseForSingleException() {
    String errorMessage = "Amazon Code deploy exception";
    AmazonCodeDeployException amazonServiceException = new AmazonCodeDeployException(errorMessage);
    WingsException processedException = exceptionManager.processException(amazonServiceException);

    assertThat(processedException).isNotNull();
    assertThat(processedException instanceof InvalidRequestException).isTrue();
    assertThat(processedException.getMessage().equals(amazonServiceException.getMessage())).isTrue();
  }

  //    @Test
  //    @Owner(developers = MOHIT_GARG)
  //    @Category(UnitTests.class)
  //    public void testUnhandledException() {
  //        String errorMessage = "Unknown Runtime exception";
  //        RuntimeException exception = new RuntimeException(errorMessage);
  //        DelegateResponseData delegateResponseData = delegateExceptionManager.getResponseData(exception, null, true);
  //        assertThat(delegateResponseData).isNotNull();
  //
  //        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
  //        assertThat(errorNotifyResponseData.getException()).isNotNull();
  //        assertThat(errorNotifyResponseData.getException() instanceof GeneralException).isTrue();
  //        assertThat(errorNotifyResponseData.getException().getMessage().equals(errorMessage)).isTrue();
  //    }
  //
  //    @Test
  //    @Owner(developers = MOHIT_GARG)
  //    @Category(UnitTests.class)
  //    public void testExceptionWhenFrameworkUnsupported() {
  //        RuntimeException exception = new RuntimeException("Unknown Runtime exception");
  //        DelegateResponseData delegateResponseData = delegateExceptionManager.getResponseData(exception, null,
  //        false); assertThat(delegateResponseData).isNotNull();
  //
  //        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
  //        assertThat(errorNotifyResponseData.getException()).isNull();
  //        assertThat(errorNotifyResponseData.getErrorMessage().equals(ExceptionUtils.getMessage(exception))).isTrue();
  //        assertThat(errorNotifyResponseData.getFailureTypes().equals(ExceptionUtils.getFailureTypes(exception))).isTrue();
  //    }
  //
  //    @Test
  //    @Owner(developers = MOHIT_GARG)
  //    @Category(UnitTests.class)
  //    public void testCascadedException() {
  //        AmazonCodeDeployException amazonServiceException = new AmazonCodeDeployException("Amazon Code deploy
  //        exception"); InvalidArtifactServerException invalidArtifactServerException =
  //                new InvalidArtifactServerException("Invalid Artifact Server", amazonServiceException);
  //
  //        DelegateResponseData delegateResponseData =
  //                delegateExceptionManager.getResponseData(invalidArtifactServerException, null, true);
  //        assertThat(delegateResponseData).isNotNull();
  //
  //        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
  //        assertThat(errorNotifyResponseData.getException()).isNotNull();
  //
  //        WingsException exception = errorNotifyResponseData.getException();
  //        assertThat(exception instanceof InvalidArtifactServerException).isTrue();
  //        Exception cause = (Exception) exception.getCause();
  //        assertThat(cause instanceof InvalidRequestException).isTrue();
  //    }
  //
  //    @Test
  //    @Owner(developers = MOHIT_GARG)
  //    @Category(UnitTests.class)
  //    public void testWingsExceptionNotRegisteredWithKryo() {
  //        String errorMessage = "Random wings exception not registered to Kryo";
  //        HourAggregationException hourAggregationException = new HourAggregationException(errorMessage, null);
  //
  //        DelegateResponseData delegateResponseData =
  //                delegateExceptionManager.getResponseData(hourAggregationException, null, true);
  //        assertThat(delegateResponseData).isNotNull();
  //
  //        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
  //        assertThat(errorNotifyResponseData.getException()).isNotNull();
  //
  //        WingsException exception = errorNotifyResponseData.getException();
  //        assertThat(exception instanceof KryoHandlerNotFoundException).isTrue();
  //        assertThat(exception.getMessage().equals(errorMessage)).isTrue();
  //    }
  //
  //    @Test
  //    @Owner(developers = MOHIT_GARG)
  //    @Category(UnitTests.class)
  //    public void testThrowable() {
  //        Throwable throwable = new Throwable("Throwable  exception");
  //        DelegateResponseData delegateResponseData = delegateExceptionManager.getResponseData(throwable, null,
  //        false); assertThat(delegateResponseData).isNotNull();
  //
  //        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
  //        assertThat(errorNotifyResponseData.getException()).isNull();
  //        assertThat(errorNotifyResponseData.getErrorMessage().equals(ExceptionUtils.getMessage(throwable))).isTrue();
  //        assertThat(errorNotifyResponseData.getFailureTypes().equals(ExceptionUtils.getFailureTypes(throwable))).isTrue();
  //    }
  //
  //    @Test
  //    @Owner(developers = MOHIT_GARG)
  //    @Category(UnitTests.class)
  //    public void testCascadedExceptionNotRegisteredWithKryo() {
  //        String errorMessage1 = "Error Message 1";
  //        String errorMessage2 = "Error Message 2";
  //        String errorMessage3 = "Error Message 3";
  //        Exception cascadedException = new HintException(
  //                errorMessage1, new HourAggregationException(errorMessage2, new ExplanationException(errorMessage3,
  //                null)));
  //
  //        DelegateResponseData delegateResponseData = delegateExceptionManager.getResponseData(cascadedException,
  //        null, true); assertThat(delegateResponseData).isNotNull();
  //
  //        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
  //        assertThat(errorNotifyResponseData.getException()).isNotNull();
  //
  //        WingsException exception = errorNotifyResponseData.getException();
  //        assertThat(exception instanceof HintException).isTrue();
  //        assertThat(exception.getMessage().equals(errorMessage1)).isTrue();
  //        exception = (WingsException) exception.getCause();
  //        assertThat(exception instanceof KryoHandlerNotFoundException).isTrue();
  //        assertThat(exception.getMessage().equals(errorMessage2)).isTrue();
  //        exception = (WingsException) exception.getCause();
  //        assertThat(exception instanceof ExplanationException).isTrue();
  //        assertThat(exception.getMessage().equals(errorMessage3)).isTrue();
  //    }
  //
  //    public static class RandomRuntimeException extends RuntimeException {
  //        public RandomRuntimeException(String message) {
  //            super(message);
  //        }
  //    }
}
