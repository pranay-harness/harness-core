package software.wings.exception;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
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
  private final Set<ErrorCode> logIgnoredErrorCodes =
      Sets.newHashSet(ErrorCode.INVALID_TOKEN, ErrorCode.EXPIRED_TOKEN, ErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND);

  /**
   * {@inheritDoc}
   */
  @Override
  public Response toResponse(WingsException ex) {
    if (ex.getResponseMessageList().stream().noneMatch(
            responseMessage -> logIgnoredErrorCodes.contains(responseMessage.getCode()))) {
      logger.error("Exception occurred", ex);
    }
    return Response.status(resolveHttpStatus(ex.getResponseMessageList()))
        .entity(aRestResponse()
                    .withResponseMessages(ex.getResponseMessageList()
                                              .stream()
                                              .map(responseMessage
                                                  -> ResponseCodeCache.getInstance().getResponseMessage(
                                                      responseMessage.getCode(), ex.getParams()))
                                              .collect(toList()))
                    .build())
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
