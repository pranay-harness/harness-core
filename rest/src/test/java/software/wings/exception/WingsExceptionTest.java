package software.wings.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ResponseMessage.Acuteness.ALERTING;
import static software.wings.beans.ResponseMessage.Acuteness.HARMLESS;
import static software.wings.beans.ResponseMessage.Acuteness.IGNORABLE;
import static software.wings.beans.ResponseMessage.Acuteness.SERIOUS;
import static software.wings.exception.WingsException.Scenario.API_CALL;
import static software.wings.exception.WingsException.Scenario.BACKGROUND_JOB;

import io.harness.CategoryTest;
import io.harness.category.FastUnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ErrorCode;

public class WingsExceptionTest extends CategoryTest {
  @Test
  @Category(FastUnitTests.class)
  public void constructionShouldCreateAtLeastOneResponseMessageTest() {
    assertThat(new WingsException("message").getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException("message", new Exception()).getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException(new Exception()).getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR, "message").getResponseMessageList().size()).isEqualTo(1);
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR, new Exception()).getResponseMessageList().size())
        .isEqualTo(1);
    assertThat(new WingsException(ErrorCode.UNKNOWN_ERROR).getResponseMessageList().size()).isEqualTo(1);
  }

  @Test
  @Category(FastUnitTests.class)
  public void testShouldLog() {
    assertThat(WingsException.shouldLog(API_CALL, SERIOUS)).isTrue();
    assertThat(WingsException.shouldLog(API_CALL, ALERTING)).isFalse();
    assertThat(WingsException.shouldLog(API_CALL, HARMLESS)).isFalse();
    assertThat(WingsException.shouldLog(API_CALL, IGNORABLE)).isTrue();

    assertThat(WingsException.shouldLog(BACKGROUND_JOB, SERIOUS)).isTrue();
    assertThat(WingsException.shouldLog(BACKGROUND_JOB, ALERTING)).isTrue();
    assertThat(WingsException.shouldLog(BACKGROUND_JOB, HARMLESS)).isTrue();
    assertThat(WingsException.shouldLog(BACKGROUND_JOB, IGNORABLE)).isFalse();
  }

  @Test
  @Category(FastUnitTests.class)
  public void testShouldPropagate() {
    assertThat(WingsException.shouldPropagate(API_CALL, SERIOUS)).isTrue();
    assertThat(WingsException.shouldPropagate(API_CALL, ALERTING)).isTrue();
    assertThat(WingsException.shouldPropagate(API_CALL, HARMLESS)).isTrue();
    assertThat(WingsException.shouldPropagate(API_CALL, IGNORABLE)).isFalse();

    assertThat(WingsException.shouldPropagate(BACKGROUND_JOB, SERIOUS)).isFalse();
    assertThat(WingsException.shouldPropagate(BACKGROUND_JOB, ALERTING)).isFalse();
    assertThat(WingsException.shouldPropagate(BACKGROUND_JOB, HARMLESS)).isFalse();
    assertThat(WingsException.shouldPropagate(BACKGROUND_JOB, IGNORABLE)).isFalse();
  }

  @Test
  public void collectResponseMessages() {
    final WingsException exception =
        new WingsException(DEFAULT_ERROR_CODE, new Exception(new WingsException(DEFAULT_ERROR_CODE)));
    assertThat(exception.getResponseMessageList().size()).isEqualTo(2);
  }
}
