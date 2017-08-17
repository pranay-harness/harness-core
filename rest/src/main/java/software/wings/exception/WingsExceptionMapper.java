package software.wings.exception;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static software.wings.beans.ResponseMessage.ResponseTypeEnum.ERROR;
import static software.wings.beans.ResponseMessage.ResponseTypeEnum.WARN;
import static software.wings.beans.RestResponse.Builder.aRestResponse;

import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.common.cache.ResponseCodeCache;

import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Created by peeyushaggarwal on 4/4/16.
 */
public class WingsExceptionMapper implements ExceptionMapper<WingsException> {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Set<ErrorCode> logIgnoredErrorCodes = Sets.newHashSet(ErrorCode.INVALID_TOKEN,
      ErrorCode.INVALID_CREDENTIAL, ErrorCode.EXPIRED_TOKEN, ErrorCode.USER_DOES_NOT_EXIST,
      ErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.UNAVAILABLE_DELEGATES);

  /**
   * {@inheritDoc}
   */
  @Override
  public Response toResponse(WingsException ex) {
    List<ResponseMessage> responseMessages =
        ex.getResponseMessageList()
            .stream()
            .map(responseMessage
                -> ResponseCodeCache.getInstance().getResponseMessage(responseMessage.getCode(), ex.getParams()))
            .collect(toList());
    if (responseMessages.stream().noneMatch(
            responseMessage -> logIgnoredErrorCodes.contains(responseMessage.getCode()))) {
      String msg = "Exception occurred: " + ex.getMessage();
      if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getErrorType() == ERROR)) {
        logger.error(msg, ex);
      } else if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getErrorType() == WARN)) {
        logger.warn(msg, ex);
      } else {
        logger.info(msg, ex);
      }
      responseMessages.forEach(responseMessage -> {
        switch (responseMessage.getErrorType()) {
          case INFO:
            logger.info(responseMessage.toString());
            break;
          case WARN:
            logger.warn(responseMessage.toString());
            break;
          case ERROR:
            logger.error(responseMessage.toString());
        }
      });
    }
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
