package io.harness.ngtriggers.resource;

import com.google.inject.Inject;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngtriggers.helpers.NGTriggerWebhookExecutionHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("triggers")
@Path("triggers/webhook")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class NGTriggerWebhookExecutionResource {
  NGTriggerWebhookExecutionHelper triggerWebhookExecutionHelper;

  @GET
  @Path("/execute")
  @ApiOperation(value = "Execute target after parsing payload", nickname = "executeTriggerTarget")
  public ResponseDTO<Boolean> executeTriggerTarget(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier, @NotNull String eventPayload) {
    return ResponseDTO.newResponse(
        triggerWebhookExecutionHelper.parsePayloadAndExecute(accountIdentifier, eventPayload));
  }
}
