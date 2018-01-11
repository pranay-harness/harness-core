package software.wings.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.ResponseMessage.Acuteness.ALERTING;
import static software.wings.beans.ResponseMessage.Acuteness.HARMLESS;
import static software.wings.beans.ResponseMessage.Acuteness.IGNORABLE;
import static software.wings.beans.ResponseMessage.Acuteness.SERIOUS;
import static software.wings.exception.WingsException.Scenario.API_CALL;
import static software.wings.exception.WingsException.Scenario.MAINTENANCE_JOB;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.BasicTest;
import software.wings.beans.ErrorCode;
import software.wings.category.FastUnitTests;

public class WingsExceptionTest extends BasicTest {
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

    assertThat(WingsException.shouldLog(MAINTENANCE_JOB, SERIOUS)).isTrue();
    assertThat(WingsException.shouldLog(MAINTENANCE_JOB, ALERTING)).isTrue();
    assertThat(WingsException.shouldLog(MAINTENANCE_JOB, HARMLESS)).isTrue();
    assertThat(WingsException.shouldLog(MAINTENANCE_JOB, IGNORABLE)).isFalse();
  }

  @Test
  @Category(FastUnitTests.class)
  public void testShouldPropagate() {
    assertThat(WingsException.shouldPropagate(API_CALL, SERIOUS)).isTrue();
    assertThat(WingsException.shouldPropagate(API_CALL, ALERTING)).isTrue();
    assertThat(WingsException.shouldPropagate(API_CALL, HARMLESS)).isTrue();
    assertThat(WingsException.shouldPropagate(API_CALL, IGNORABLE)).isFalse();

    assertThat(WingsException.shouldPropagate(MAINTENANCE_JOB, SERIOUS)).isFalse();
    assertThat(WingsException.shouldPropagate(MAINTENANCE_JOB, ALERTING)).isFalse();
    assertThat(WingsException.shouldPropagate(MAINTENANCE_JOB, HARMLESS)).isFalse();
    assertThat(WingsException.shouldPropagate(MAINTENANCE_JOB, IGNORABLE)).isFalse();
  }
}
