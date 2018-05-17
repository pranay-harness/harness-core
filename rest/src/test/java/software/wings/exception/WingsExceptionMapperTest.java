package software.wings.exception;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ErrorCode.INVALID_ARTIFACT_SOURCE;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.exception.WingsException.USER;

import io.harness.CategoryTest;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import software.wings.beans.ResponseMessage;
import software.wings.common.cache.ResponseCodeCache;

import javax.ws.rs.core.Response;

public class WingsExceptionMapperTest extends CategoryTest {
  @Test
  public void sanity() {
    final WingsException exception = new WingsException(DEFAULT_ERROR_CODE);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(mapper, "logger", mockLogger);

    final Response response = mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger)
        .error("Response message: An error has occurred. Please contact the Harness support team.\n"
                + "Exception occurred: DEFAULT_ERROR_CODE",
            exception);
  }

  @Test
  public void missingParameter() {
    final WingsException exception = new WingsException(INVALID_ARTIFACT_SOURCE, USER);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(ResponseCodeCache.getInstance(), "logger", mockLogger);

    final Response response = mapper.toResponse(exception);
    verify(mockLogger).error("Insufficient parameter from [] in message \"Invalid Artifact Source:${name}.${reason}\"");
  }

  @Test
  public void overrideMessage() {
    final ResponseMessage message = aResponseMessage().code(DEFAULT_ERROR_CODE).message("Override message").build();

    final WingsException exception = new WingsException(message);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(mapper, "logger", mockLogger);
    Whitebox.setInternalState(ResponseCodeCache.getInstance(), "logger", mockLogger);

    final Response response = mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger)
        .error("Response message: An error has occurred. Please contact the Harness support team.\n"
                + "Exception occurred: Override message",
            exception);
  }

  @Test
  public void shouldNotLogHarmless() {
    final WingsException exception = new WingsException(DEFAULT_ERROR_CODE, USER);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(mapper, "logger", mockLogger);

    final Response response = mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger, never()).error(any());
    inOrder.verify(mockLogger, never()).error(any(), (Object) anyObject());
    inOrder.verify(mockLogger, never()).error(any(), (Throwable) any());
  }
}
