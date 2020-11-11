package io.harness.ngtriggers.resource;

import com.google.inject.Inject;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.ngtriggers.service.WebhookConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;
import java.util.Map;

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
public class NGTriggerWebhookConfigResource {
  @GET
  @Path("/sourceRepos")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getSourceRepoToEvent")
  public ResponseDTO<Map<WebhookSourceRepo, List<WebhookEvent>>> getSourceRepoToEvent() {
    return ResponseDTO.newResponse(WebhookConfigService.getSourceRepoToEvent());
  }

  @GET
  @Path("/actions")
  @ApiOperation(value = "Get Actions for event type and source", nickname = "getActionsList")
  public ResponseDTO<List<WebhookAction>> getActionsList(
      @NotNull @QueryParam("sourceRepo") WebhookSourceRepo sourceRepo,
      @NotNull @QueryParam("event") WebhookEvent event) {
    return ResponseDTO.newResponse(WebhookConfigService.getActionsList(sourceRepo, event));
  }
}
