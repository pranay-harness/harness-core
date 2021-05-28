package io.harness.ng.webhook;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.constants.Constants.UNRECOGNIZED_WEBHOOK;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HookEventType;
import io.harness.delegate.task.scm.ScmGitWebhookTaskResponseData;
import io.harness.gitsync.common.impl.ScmDelegateFacilitatorServiceImpl;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.webhook.entities.WebhookEvent;
import io.harness.ng.webhook.services.api.WebhookService;
import io.harness.security.annotations.PublicApi;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
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
public class NgWebhookResource {
  private WebhookService webhookService;
  private WebhookHelper webhookHelper;
  private ScmDelegateFacilitatorServiceImpl scmDelegateFacilitatorService;

  @POST
  @ApiOperation(value = "accept webhook event", nickname = "webhookEndpoint")
  @PublicApi
  public ResponseDTO<String> processWebhookEvent(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier, @NotNull String eventPayload,
      @Context HttpHeaders httpHeaders) {
    WebhookEvent eventEntity = webhookHelper.toNGTriggerWebhookEvent(accountIdentifier, eventPayload, httpHeaders);
    if (eventEntity != null) {
      WebhookEvent newEvent = webhookService.addEventToQueue(eventEntity);
      return ResponseDTO.newResponse(newEvent.getUuid());
    } else {
      return ResponseDTO.newResponse(UNRECOGNIZED_WEBHOOK);
    }
  }

  @POST
  @Path("UpsertWebhook")
  @ApiOperation(value = "Upsert a webhook event", nickname = "webhookUpsert")
  @PublicApi
  public ResponseDTO<ScmGitWebhookTaskResponseData> upsertWebhook(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.CONNECTOR_IDENTIFIER_REF) String connectorIdenfiierRef,
      @NotNull @QueryParam(WebhookConstants.TARGET) String target,
      @NotNull @QueryParam(WebhookConstants.HOOK_EVENT_TYPE) HookEventType hookEventType,
      @QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL) {
    final ScmGitWebhookTaskResponseData scmGitWebhookTaskResponseData = webhookService.upsertWebhook(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdenfiierRef, target, hookEventType, repoURL);
    return ResponseDTO.newResponse(scmGitWebhookTaskResponseData);
  }
}
