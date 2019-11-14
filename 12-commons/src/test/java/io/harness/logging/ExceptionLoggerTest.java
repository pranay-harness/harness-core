package io.harness.logging;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.EnumSet;
import java.util.List;

public class ExceptionLoggerTest extends CategoryTest {
  @Before()
  public void setup() {
    initializeLogging();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCollectResponseMessages() {
    final WingsException exception =
        new WingsException(DEFAULT_ERROR_CODE, new Exception(new WingsException(INVALID_ARGUMENT)));
    assertThat(ExceptionLogger.getResponseMessageList(exception, REST_API).size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testExcludeReportTarget() {
    final WingsException exception =
        new WingsException(DEFAULT_ERROR_CODE, new Exception(new WingsException(DEFAULT_ERROR_CODE)));

    assertThat(exception.getReportTargets()).contains(REST_API);
    assertThat(((WingsException) exception.getCause().getCause()).getReportTargets()).contains(REST_API);

    exception.excludeReportTarget(DEFAULT_ERROR_CODE, EnumSet.of(REST_API));

    assertThat(exception.getReportTargets()).doesNotContain(REST_API);
    assertThat(((WingsException) exception.getCause().getCause()).getReportTargets()).doesNotContain(REST_API);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCalculateErrorMessage() {
    WingsException exception = new WingsException(DEFAULT_ERROR_CODE);

    exception.addContext(String.class, "test");
    exception.addContext(Integer.class, 0);

    final List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(exception, LOG_SYSTEM);
    assertThat(ExceptionLogger.calculateErrorMessage(exception, responseMessages))
        .isEqualTo("Response message: An error has occurred. Please contact the Harness support team.\n"
            + "Exception occurred: DEFAULT_ERROR_CODE");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCalculateErrorMessageForChain() {
    WingsException innerException = new WingsException(DEFAULT_ERROR_CODE);
    innerException.addContext(String.class, "test");

    WingsException outerException = new WingsException(DEFAULT_ERROR_CODE, innerException);
    outerException.addContext(Integer.class, 0);

    final List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(outerException, LOG_SYSTEM);
    assertThat(ExceptionLogger.calculateErrorMessage(outerException, responseMessages))
        .isEqualTo("Response message: An error has occurred. Please contact the Harness support team.\n"
            + "Exception occurred: DEFAULT_ERROR_CODE");
  }
}
