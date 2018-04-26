package software.wings.exception;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static software.wings.beans.RestResponse.Builder.aRestResponse;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.exception.WingsException.ReportTarget.REST_API;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;

import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Created by peeyushaggarwal on 4/4/16.
 */
public class WingsExceptionMapper implements ExceptionMapper<WingsException> {
  private static Logger logger = LoggerFactory.getLogger(WingsExceptionMapper.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public Response toResponse(WingsException ex) {
    ex.logProcessedMessages(MANAGER, logger);
    List<ResponseMessage> responseMessages = ex.getResponseMessageList(REST_API);

    return Response.status(resolveHttpStatus(responseMessages))
        .entity(aRestResponse().withResponseMessages(responseMessages).build())
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
