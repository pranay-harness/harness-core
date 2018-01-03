package software.wings.exception;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ErrorCode.INVALID_ARTIFACT_SOURCE;
import static software.wings.beans.ResponseMessage.Acuteness.HARMLESS;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InOrder;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import software.wings.BasicTest;
import software.wings.beans.ResponseMessage;
import software.wings.category.FastUnitTests;
import software.wings.category.feature.CoreTests;
import software.wings.category.element.UnitTests;
import software.wings.common.cache.ResponseCodeCache;

import java.util.Arrays;
import javax.ws.rs.core.Response;

public class WingsExceptionMapperTest extends BasicTest {
  @Test
  @Category({FastUnitTests.class, CoreTests.class})
  public void sanity() {
    final WingsException exception = new WingsException(DEFAULT_ERROR_CODE);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(exception, "logger", mockLogger);
    Whitebox.setInternalState(mapper, "logger", mockLogger);

    final Response response = mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger).error("Exception occurred: DEFAULT_ERROR_CODE", exception);
    inOrder.verify(mockLogger).error(matches(".*An error has occurred. Please contact the Harness support team.*"));
  }

  @Test
  @Category({FastUnitTests.class, CoreTests.class})
  public void missingParameter() {
    final WingsException exception = new WingsException(INVALID_ARTIFACT_SOURCE);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(exception, "logger", mockLogger);
    Whitebox.setInternalState(mapper, "logger", mockLogger);
    Whitebox.setInternalState(ResponseCodeCache.getInstance(), "logger", mockLogger);

    final Response response = mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger)
        .error("Insufficient parameter from [] in message \"Invalid Artifact Source:${name}.${reason}\"");
    inOrder.verify(mockLogger).error("Exception occurred: INVALID_ARTIFACT_SOURCE", exception);
  }

  @Test
  @Category({FastUnitTests.class, CoreTests.class})
  public void overrideMessage() {
    final ResponseMessage message = aResponseMessage().code(DEFAULT_ERROR_CODE).message("Override message").build();

    final WingsException exception = new WingsException(Arrays.asList(message), "Dummy message", (Throwable) null);
    final WingsExceptionMapper mapper = new WingsExceptionMapper();

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(exception, "logger", mockLogger);
    Whitebox.setInternalState(mapper, "logger", mockLogger);
    Whitebox.setInternalState(ResponseCodeCache.getInstance(), "logger", mockLogger);

    final Response response = mapper.toResponse(exception);

    InOrder inOrder = inOrder(mockLogger);
    inOrder.verify(mockLogger).error("The provided response message \"Override message\" will be overridden!");
    inOrder.verify(mockLogger).error("Exception occurred: Dummy message", exception);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotLogHarmless() {
    final ResponseMessage message = aResponseMessage().code(DEFAULT_ERROR_CODE).acuteness(HARMLESS).build();

    final WingsException exception = new WingsException(Arrays.asList(message), "Dummy message", (Throwable) null);
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
