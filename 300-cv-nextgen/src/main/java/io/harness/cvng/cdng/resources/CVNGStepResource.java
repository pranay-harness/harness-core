package io.harness.cvng.cdng.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.cdng.beans.InputSetTemplateRequest;
import io.harness.cvng.cdng.beans.InputSetTemplateResponse;
import io.harness.cvng.cdng.services.api.CVNGStepService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("verify-step")
@Path("/verify-step")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class CVNGStepResource {
  @Inject private CVNGStepService cvngStepService;
  @POST
  @Path("input-set-template")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Given a template Add verify step to it if required", nickname = "inputSetTemplate")
  public ResponseDTO<InputSetTemplateResponse> updateInputSetTemplate(
      @NotNull @QueryParam("accountId") String accountId, InputSetTemplateRequest inputSetTemplateRequest) {
    return ResponseDTO.newResponse(
        InputSetTemplateResponse.builder()
            .inputSetTemplateYaml(cvngStepService.getUpdatedInputSetTemplate(inputSetTemplateRequest.getPipelineYaml()))
            .build());
  }
}
