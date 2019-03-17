package software.wings.exception;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SOURCE;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.eraro.MessageManager;
import io.harness.exception.WingsException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InOrder;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import software.wings.WingsBaseTest;

public class WingsExceptionMapperTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void sanity() {
    final WingsException exception = WingsException.builder().code(DEFAULT_ERROR_CODE).build();
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(mapper, "logger", mockLogger);

    mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger)
        .error("Response message: An error has occurred. Please contact the Harness support team.\n"
                + "Exception occurred: DEFAULT_ERROR_CODE",
            exception);
  }

  @Test
  @Category(UnitTests.class)
  public void missingParameter() throws IllegalAccessException {
    final WingsException exception = new WingsException(INVALID_ARTIFACT_SOURCE, USER);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    setStaticFieldValue(MessageManager.class, "logger", mockLogger);

    mapper.toResponse(exception);
    verify(mockLogger, times(2))
        .info("Insufficient parameter from [] in message \"Invalid Artifact Source:${name}.${reason}\"");
  }

  @Test
  @Category(UnitTests.class)
  public void overrideMessage() throws IllegalAccessException {
    final WingsException exception =
        WingsException.builder().message("Override message").code(DEFAULT_ERROR_CODE).build();
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    setStaticFieldValue(WingsExceptionMapper.class, "logger", mockLogger);
    setStaticFieldValue(MessageManager.class, "logger", mockLogger);

    mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger)
        .error("Response message: An error has occurred. Please contact the Harness support team.\n"
                + "Exception occurred: Override message",
            exception);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotLogHarmless() {
    final WingsException exception = new WingsException(DEFAULT_ERROR_CODE, USER);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(mapper, "logger", mockLogger);

    mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger, never()).error(any());
    inOrder.verify(mockLogger, never()).error(any(), (Object) anyObject());
    inOrder.verify(mockLogger, never()).error(any(), (Throwable) any());
  }

  @Test
  @Category(UnitTests.class)
  public void recursiveParamTest() {
    WingsException exception = new WingsException(VAULT_OPERATION_ERROR, USER);
    exception.addParam("reason", "recursive call to ${reason}");

    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(mapper, "logger", mockLogger);

    mapper.toResponse(exception); // should not throw.
  }
}
