package software.wings.exception;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static software.wings.beans.ResponseMessage.Acuteness.SERIOUS;
import static software.wings.beans.ResponseMessage.Level.ERROR;
import static software.wings.beans.ResponseMessage.Level.WARN;
import static software.wings.beans.RestResponse.Builder.aRestResponse;
import static software.wings.utils.Switch.unhandled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.Level;
import software.wings.common.cache.ResponseCodeCache;

import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Created by peeyushaggarwal on 4/4/16.
 */
public class WingsExceptionMapper implements ExceptionMapper<WingsException> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  public Response toResponse(WingsException ex) {
    List<ResponseMessage> responseMessages =
        ex.getResponseMessageList()
            .stream()
            .map(responseMessage -> ResponseCodeCache.getInstance().rebuildMessage(responseMessage, ex.getParams()))
            .collect(toList());

    if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getAcuteness() == SERIOUS)) {
      String msg = "Exception occurred: " + ex.getMessage();
      if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == ERROR)) {
        logger.error(msg, ex);
      } else if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == WARN)) {
        logger.warn(msg, ex);
      } else {
        logger.info(msg, ex);
      }
    }

    responseMessages.stream()
        .filter(responseMessage -> responseMessage.getAcuteness() == SERIOUS)
        .forEach(responseMessage -> {
          final Level errorType = responseMessage.getLevel();
          switch (errorType) {
            case INFO:
              logger.info(responseMessage.toString());
              break;
            case WARN:
              logger.warn(responseMessage.toString());
              break;
            case ERROR:
              logger.error(responseMessage.toString());
              break;
            default:
              unhandled(errorType);
          }
        });

    return Response.status(resolveHttpStatus(ex.getResponseMessageList()))
        .entity(aRestResponse().withResponseMessages(responseMessages).build())
        .build();
  }

  private Status resolveHttpStatus(List<ResponseMessage> responseMessageList) {
    ErrorCode errorCode = null;
    if (responseMessageList != null && responseMessageList.size() > 0) {
      errorCode = responseMessageList.get(responseMessageList.size() - 1).getCode();
    }
    if (errorCode != null) {
      return errorCode.getStatus();
    } else {
      return INTERNAL_SERVER_ERROR;
    }
  }
}
