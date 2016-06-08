package software.wings.exception;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.ErrorConstants.INVALID_ARGUMENT;
import static software.wings.beans.ResponseMessage.Builder.aResponseMessage;
import static software.wings.beans.RestResponse.Builder.aRestResponse;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.validation.ConstraintViolations;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by peeyushaggarwal on 6/8/16.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
  @Override
  public Response toResponse(ConstraintViolationException exception) {
    ImmutableList<String> errors =
        ImmutableList.copyOf(exception.getConstraintViolations()
                                 .stream()
                                 .map(v -> processMessage(v, ConstraintMessage.getMessage(v)))
                                 .collect(toList()));

    if (errors.size() == 0) {
      errors = ImmutableList.of(Strings.nullToEmpty(exception.getMessage()));
    }

    return Response.status(ConstraintViolations.determineStatus(exception.getConstraintViolations()))
        .entity(toRestResponse(errors))
        .build();
  }

  private RestResponse toRestResponse(ImmutableList<String> errors) {
    return aRestResponse()
        .withResponseMessages(errors.stream().map(this ::errorMessageToResponseMessage).collect(toList()))
        .build();
  }

  private ResponseMessage errorMessageToResponseMessage(String s) {
    return aResponseMessage().withCode(INVALID_ARGUMENT).withErrorType(ResponseTypeEnum.ERROR).withMessage(s).build();
  }

  private String processMessage(ConstraintViolation<?> v, String message) {
    ConstraintDescriptor constraintDescriptor = v.getConstraintDescriptor();
    boolean replaceArg = false;
    for (Path.Node node : v.getPropertyPath()) {
      switch (node.getKind()) {
        case METHOD: {
          message = message.replace(node + ".", "");
          replaceArg = true;
          break;
        }
        case PARAMETER: {
          if (replaceArg) {
            message = message.replace(node + ".", "");
          }
          break;
        }
        default: { break; }
      }
    }
    return message;
  }
}
