package software.wings.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.exception.WingsException.ReportTarget.REST_API;

import io.harness.CategoryTest;
import org.junit.Test;
import software.wings.beans.ErrorCode;

public class WingsExceptionTest extends CategoryTest {
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
        new WingsException(DEFAULT_ERROR_CODE, new Exception(new WingsException(DEFAULT_ERROR_CODE)));
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
}
