package software.wings.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.REST_API;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;

import java.util.List;

public class WingsExceptionTest extends WingsBaseTest {
  @Test
  public void constructionShouldCreateAtLeastOneResponseMessageTest() {
    assertThat(new WingsException("message").getResponseMessage()).isNotNull();
    assertThat(new WingsException("message", new Exception()).getResponseMessage()).isNotNull();
    assertThat(new WingsException(new Exception()).getResponseMessage()).isNotNull();
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR, "message").getResponseMessage()).isNotNull();
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR, new Exception()).getResponseMessage()).isNotNull();
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR).getResponseMessage()).isNotNull();
  }

  @Test
  public void testCollectResponseMessages() {
    final WingsException exception =
        new WingsException(DEFAULT_ERROR_CODE, new Exception(new WingsException(INVALID_ARGUMENT)));
    assertThat(exception.getResponseMessageList(REST_API).size()).isEqualTo(2);
  }

  @Test
  public void testExcludeReportTarget() {
    final WingsException exception =
        new WingsException(DEFAULT_ERROR_CODE, new Exception(new WingsException(DEFAULT_ERROR_CODE)));

    assertThat(exception.getReportTargets()).contains(REST_API);
    assertThat(((WingsException) exception.getCause().getCause()).getReportTargets()).contains(REST_API);

    exception.excludeReportTarget(DEFAULT_ERROR_CODE, REST_API);

    assertThat(exception.getReportTargets()).doesNotContain(REST_API);
    assertThat(((WingsException) exception.getCause().getCause()).getReportTargets()).doesNotContain(REST_API);
  }

  @Test
  public void testCalculateErrorMessage() {
    WingsException exception = new WingsException(DEFAULT_ERROR_CODE);

    exception.addContext(String.class, "test");
    exception.addContext(Integer.class, 0);

    final List<ResponseMessage> responseMessages = exception.getResponseMessageList(LOG_SYSTEM);
    assertThat(exception.calculateErrorMessage(responseMessages))
        .isEqualTo("Response message: An error has occurred. Please contact the Harness support team.\n"
            + "Context objects: java.lang.Integer: 0\n"
            + "                 java.lang.String: test\n"
            + "Exception occurred: DEFAULT_ERROR_CODE");
  }

  @Test
  public void testCalculateErrorMessageForChain() {
    WingsException innerException = new WingsException(DEFAULT_ERROR_CODE);
    innerException.addContext(String.class, "test");

    WingsException outerException = new WingsException(DEFAULT_ERROR_CODE, innerException);
    outerException.addContext(Integer.class, 0);

    final List<ResponseMessage> responseMessages = outerException.getResponseMessageList(LOG_SYSTEM);
    assertThat(outerException.calculateErrorMessage(responseMessages))
        .isEqualTo("Response message: An error has occurred. Please contact the Harness support team.\n"
            + "Context objects: java.lang.Integer: 0\n"
            + "                 java.lang.String: test\n"
            + "Exception occurred: DEFAULT_ERROR_CODE");
  }
}
