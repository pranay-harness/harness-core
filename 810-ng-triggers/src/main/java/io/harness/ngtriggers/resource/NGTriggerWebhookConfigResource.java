package io.harness.ngtriggers.resource;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.constants.Constants.UNRECOGNIZED_WEBHOOK;
import static io.harness.ngtriggers.Constants.TRIGGER_KEY;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngtriggers.beans.dto.WebhookEventProcessingDetails;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.action.BitbucketPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.action.GitlabPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabTriggerEvent;
import io.harness.ngtriggers.helpers.WebhookConfigHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.security.annotations.PublicApi;
import io.harness.utils.YamlPipelineUtils;

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
@Produces({"application/json", "application/yaml", "text/plain"})
@Consumes({"application/json", "application/yaml", "text/plain"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
@OwnedBy(PIPELINE)
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
  @Path("/gitTriggerEventDetails")
  @ApiOperation(value = "Get trigger git actions with Events", nickname = "getGitTriggerEventDetails")
  public ResponseDTO<Map<String, Map<String, List<String>>>> getGitTriggerEventDetails() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGitTriggerEventDetails());
  }

  @GET
  @Path("/webhookTriggerTypes")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getWebhookTriggerTypes")
  public ResponseDTO<List<WebhookTriggerType>> getWebhookTriggerTypes() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getWebhookTriggerType());
  }

  @GET
  @Path("/githubTriggerEvents")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGithubTriggerEvents")
  public ResponseDTO<List<GithubTriggerEvent>> getGithubTriggerEvents() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGithubTriggerEvents());
  }

  @GET
  @Path("/githubPRActions")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGithubPRActions")
  public ResponseDTO<List<GithubPRAction>> getGithubPRActions() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGithubPRAction());
  }

  @GET
  @Path("/githubIssueCommentActions")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGithubIssueCommentActions")
  public ResponseDTO<List<GithubIssueCommentAction>> getGithubIssueCommentActions() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGithubIssueCommentAction());
  }

  @GET
  @Path("/gitlabTriggerEvents")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGitlabTriggerEvents")
  public ResponseDTO<List<GitlabTriggerEvent>> getGitlabTriggerEvents() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGitlabTriggerEvents());
  }

  @GET
  @Path("/gitlabPRActions")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGitlabPRActions")
  public ResponseDTO<List<GitlabPRAction>> getGitlabTriggerActions() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGitlabPRAction());
  }

  @GET
  @Path("/bitbucketTriggerEvents")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getBitbucketTriggerEvents")
  public ResponseDTO<List<BitbucketTriggerEvent>> getBitbucketTriggerEvents() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getBitbucketTriggerEvents());
  }

  @GET
  @Path("/bitbucketPRActions")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getBitbucketPRActions")
  public ResponseDTO<List<BitbucketPRAction>> getBitbucketPRActions() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getBitbucketPRAction());
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

    TriggerWebhookEvent eventEntity =
        ngTriggerElementMapper
            .toNGTriggerWebhookEvent(accountIdentifier, orgIdentifier, projectIdentifier, eventPayload, headerConfigs)
            .build();
    if (eventEntity != null) {
      TriggerWebhookEvent newEvent = ngTriggerService.addEventToQueue(eventEntity);
      return ResponseDTO.newResponse(newEvent.getUuid());
    } else {
      return ResponseDTO.newResponse(UNRECOGNIZED_WEBHOOK);
    }
  }

  @POST
  @Path("/custom")
  @ApiOperation(value = "accept custom webhook event", nickname = "customWebhookEndpoint")
  @PublicApi
  public ResponseDTO<String> processWebhookEvent(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(TRIGGER_KEY) String triggerIdentifier, @NotNull String eventPayload,
      @Context HttpHeaders httpHeaders) {
    List<HeaderConfig> headerConfigs = new ArrayList<>();
    httpHeaders.getRequestHeaders().forEach(
        (k, v) -> headerConfigs.add(HeaderConfig.builder().key(k).values(v).build()));

    TriggerWebhookEvent eventEntity =
        ngTriggerElementMapper
            .toNGTriggerWebhookEventForCustom(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier,
                triggerIdentifier, eventPayload, headerConfigs)
            .build();
    if (eventEntity != null) {
      TriggerWebhookEvent newEvent = ngTriggerService.addEventToQueue(eventEntity);
      return ResponseDTO.newResponse(newEvent.getUuid());
    } else {
      return ResponseDTO.newResponse(UNRECOGNIZED_WEBHOOK);
    }
  }

  @GET
  @Path("/triggerProcessingDetails")
  @ApiOperation(value = "fetch webhook event details", nickname = "triggerProcessingDetails")
  @PublicApi
  public ResponseDTO<WebhookEventProcessingDetails> fetchWebhookDetails(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam("eventId") String eventId) {
    return ResponseDTO.newResponse(ngTriggerService.fetchTriggerEventHistory(accountIdentifier, eventId));
  }
}
