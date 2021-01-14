package io.harness.ngtriggers.resource;

import io.harness.NGCommonEntityConstants;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngtriggers.beans.config.HeaderConfig;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.ngtriggers.helpers.WebhookConfigHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.security.annotations.PublicApi;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api("webhook")
@Path("webhook")
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
  private final NGTriggerService ngTriggerService;
  private final NGTriggerElementMapper ngTriggerElementMapper;

  @GET
  @Path("/sourceRepos")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getSourceRepoToEvent")
  public ResponseDTO<Map<WebhookSourceRepo, List<WebhookEvent>>> getSourceRepoToEvent() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getSourceRepoToEvent());
  }

  @GET
  @Path("/actions")
  @ApiOperation(value = "Get Actions for event type and source", nickname = "getActionsList")
  public ResponseDTO<List<WebhookAction>> getActionsList(
      @NotNull @QueryParam("sourceRepo") WebhookSourceRepo sourceRepo, @NotNull @QueryParam("event") String event) {
    WebhookEvent webhookEvent;
    try {
      webhookEvent = YamlPipelineUtils.read(event, WebhookEvent.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Event: " + event + " is not valid");
    }
    return ResponseDTO.newResponse(WebhookConfigHelper.getActionsList(sourceRepo, webhookEvent));
  }

  @POST
  @Path("/trigger")
  @ApiOperation(value = "accept webhook event", nickname = "webhookEndpoint")
  @PublicApi
  public ResponseDTO<String> processWebhookEvent(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @NotNull String eventPayload,
      @Context HttpHeaders httpHeaders) {
    List<HeaderConfig> headerConfigs = new ArrayList<>();
    httpHeaders.getRequestHeaders().forEach(
        (k, v) -> headerConfigs.add(HeaderConfig.builder().key(k).values(v).build()));

    TriggerWebhookEvent eventEntity = ngTriggerElementMapper.toNGTriggerWebhookEvent(
        accountIdentifier, orgIdentifier, projectIdentifier, eventPayload, headerConfigs);
    TriggerWebhookEvent newEvent = ngTriggerService.addEventToQueue(eventEntity);
    return ResponseDTO.newResponse(newEvent.getUuid());
  }
}
