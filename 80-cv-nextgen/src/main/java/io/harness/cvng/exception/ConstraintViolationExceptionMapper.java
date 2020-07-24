package io.harness.cvng.exception;

import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Slf4j
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
  @Context ResourceInfo resourceInfo;

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    logger.error("Exception occurred: " + ExceptionUtils.getMessage(exception), exception);
    Set<ConstraintViolation<?>> constraintViolations = exception.getConstraintViolations();

    List<ValidationError> errors = new ArrayList<>();
    constraintViolations.forEach(constraintViolation -> {
      String field = null;
      for (Path.Node node : constraintViolation.getPropertyPath()) {
        field = node.getName();
      }
      errors.add(new ValidationError(field, constraintViolation.getMessage()));
    });
    return Response.status(Response.Status.BAD_REQUEST).entity(errors).type(MediaType.APPLICATION_JSON).build();
  }
}
