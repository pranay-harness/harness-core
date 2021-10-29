package io.harness.ng.core.template.exception;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.ExceptionLogger;
import io.harness.ng.core.template.TemplateInputsErrorResponseDTO;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class NGTemplateResolveExceptionMapper implements ExceptionMapper<NGTemplateResolveException> {
  @Override
  public Response toResponse(NGTemplateResolveException exception) {
    ExceptionLogger.logProcessedMessages(exception, MANAGER, log);
    TemplateInputsErrorResponseDTO errorResponseDTO = exception.getErrorResponseDTO();
    return Response.status(Response.Status.fromStatusCode(exception.getCode().getStatus().getCode()))
        .entity(errorResponseDTO)
        .build();
  }
}
