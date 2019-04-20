package software.wings.exception;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
@Slf4j
public class WingsExceptionMapper implements ExceptionMapper<WingsException> {
  @Override
  public Response toResponse(WingsException exception) {
    ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(exception, REST_API);

    ErrorCode errorCode = exception.getCode() != null ? exception.getCode() : ErrorCode.UNKNOWN_ERROR;

    return Response.status(resolveHttpStatus(responseMessages))
        .entity(aRestResponse().withResponseMessages(responseMessages).build())
        .header("X-Harness-Error", errorCode.toString())
        .build();
  }

  private Status resolveHttpStatus(List<ResponseMessage> responseMessageList) {
    ErrorCode errorCode = null;
    if (isNotEmpty(responseMessageList)) {
      errorCode = responseMessageList.get(responseMessageList.size() - 1).getCode();
    }
    if (errorCode != null) {
      return errorCode.getStatus();
    } else {
      return INTERNAL_SERVER_ERROR;
    }
  }
}
