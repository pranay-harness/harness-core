package software.wings.resources;

import static io.harness.beans.FeatureName.WEBHOOK_TRIGGER_AUTHORIZATION;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.Application;
import software.wings.beans.WebHookRequest;
import software.wings.security.annotations.ApiKeyAuthorized;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WebHookService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Api("webhooks")
@Path("/webhooks")
@Produces("application/json")
@ApiKeyAuthorized(allowEmptyApiKey = true, skipAuth = true)
public class WebHookResource {
  private WebHookService webHookService;
  private FeatureFlagService featureFlagService;
  private AppService appService;

  @Inject
  public WebHookResource(WebHookService webHookService, FeatureFlagService featureFlagService, AppService appService) {
    this.webHookService = webHookService;
    this.featureFlagService = featureFlagService;
    this.appService = appService;
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("triggers/{webHookToken}")
  public Response execute(
      @PathParam("webHookToken") String webHookToken, String eventPayload, @Context HttpHeaders httpHeaders) {
    return webHookService.executeByEvent(webHookToken, eventPayload, httpHeaders);
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("{webHookToken}")
  public Response execute(@HeaderParam(API_KEY_HEADER) String apiKey, @QueryParam("accountId") String accountId,
      @PathParam("webHookToken") String webHookToken, WebHookRequest webHookRequest) {
    if (featureFlagService.isEnabled(WEBHOOK_TRIGGER_AUTHORIZATION, accountId)) {
      String appId = webHookRequest.getApplication();
      Application application = appService.get(appId);
      if (application.getIsManualTriggerAuthorized() && isEmpty(apiKey)) {
        throw new InvalidRequestException("Api Key cannot be empty", WingsException.USER);
      }
    }
    return webHookService.execute(webHookToken, webHookRequest);
  }

  /**
   * This method is used for HTTP validation state to see if this endpoint is reachable.
   * No business logic/validation is required here, so we just return response SUCCESS.
   * @param webHookToken
   * @param webHookRequest
   * @return
   */
  @GET
  @Timed
  @ExceptionMetered
  @Path("{webHookToken}")
  public Response ping(@PathParam("webHookToken") String webHookToken, WebHookRequest webHookRequest) {
    return Response.status(Response.Status.OK).build();
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @Path("{webHookToken}/git")
  public Response executeGit(
      @PathParam("webHookToken") String webHookToken, String eventPayload, @Context HttpHeaders httpHeaders) {
    return webHookService.executeByEvent(webHookToken, eventPayload, httpHeaders);
  }
}
