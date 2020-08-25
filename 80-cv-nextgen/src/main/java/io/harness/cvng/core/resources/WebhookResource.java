package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("webhook")
@Path("webhook")
@Produces("application/json")
public class WebhookResource {
  @Inject WebhookService webhookService;

  @GET
  @Path("create-token")
  @Timed
  @ExceptionMetered
  public RestResponse<String> createToken(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("projectIdentifier") final String projectIdentifier,
      @QueryParam("orgIdentifier") final String orgIdentifier) {
    return new RestResponse<>(webhookService.createWebhookToken(projectIdentifier, orgIdentifier));
  }

  @GET
  @Path("recreate-token")
  @Timed
  @ExceptionMetered
  public RestResponse<String> recreateToken(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("projectIdentifier") final String projectIdentifier,
      @QueryParam("orgIdentifier") final String orgIdentifier) {
    return new RestResponse<>(webhookService.recreateWebhookToken(projectIdentifier, orgIdentifier));
  }

  @DELETE
  @Path("delete-token")
  @Timed
  @ExceptionMetered
  public void deleteToken(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("projectIdentifier") final String projectIdentifier,
      @QueryParam("orgIdentifier") final String orgIdentifier) {
    webhookService.deleteWebhookToken(projectIdentifier, orgIdentifier);
  }
}
