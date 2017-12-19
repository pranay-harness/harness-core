package software.wings.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.beans.ErrorCode;

public class WingsExceptionTest {
  @Test
  public void constructionShouldCreateAtLeastOneResponseMessageTest() {
    assertThat(new WingsException("message").getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException("message", new Exception()).getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException(new Exception()).getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR, "message").getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR, new Exception()).getResponseMessageList().size())
        .isEqualTo(1);
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR).getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR, "key", new Object()).getResponseMessageList().size())
        .isEqualTo(1);
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR, "key", new Object(), new Exception())
                   .getResponseMessageList()
                   .size())
        .isEqualTo(1);
  }
}
