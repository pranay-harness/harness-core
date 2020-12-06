package software.wings.exception;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.rest.RestResponse.Builder.aRestResponse;

import static java.util.stream.Collectors.toList;

import io.harness.eraro.Level;
import io.harness.eraro.ResponseMessage;
import io.harness.rest.RestResponse;

import software.wings.utils.ConstraintViolationHandlerUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by peeyushaggarwal on 6/8/16.
 */
@Provider
@Slf4j
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
  @Override
  public Response toResponse(ConstraintViolationException exception) {
    ImmutableList<String> errors = ConstraintViolationHandlerUtils.getErrorMessages(exception);

    if (errors.isEmpty()) {
      errors = ImmutableList.of(Strings.nullToEmpty(exception.getMessage()));
    }

    log.info(toRestResponse(errors).toString());

    return Response.status(Status.BAD_REQUEST).entity(toRestResponse(errors)).build();
  }

  private RestResponse toRestResponse(ImmutableList<String> errors) {
    return aRestResponse()
        .withResponseMessages(errors.stream().map(this::errorMessageToResponseMessage).collect(toList()))
        .build();
  }

  private ResponseMessage errorMessageToResponseMessage(String s) {
    return ResponseMessage.builder().code(INVALID_ARGUMENT).level(Level.ERROR).message(s).build();
  }
}
