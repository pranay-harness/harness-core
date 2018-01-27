package software.wings.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;

import io.harness.CategoryTest;
import org.junit.Test;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException.ReportTarget;

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
  public void collectResponseMessages() {
    final WingsException exception =
        new WingsException(DEFAULT_ERROR_CODE, new Exception(new WingsException(DEFAULT_ERROR_CODE)));
    assertThat(exception.getResponseMessageList(ReportTarget.USER).size()).isEqualTo(2);
  }
}
