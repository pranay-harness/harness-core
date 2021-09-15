package io.harness.cvng.core.resources;

import io.harness.cvng.core.beans.PagerDutyIncidentDTO;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Api("webhook")
@Path("webhook")
@Produces("application/json")
public class WebhookResource {
  @Inject private WebhookService webhookService;

  @POST
  @Path("pagerduty/{token}")
  @Timed
  @PublicApi
  @ExceptionMetered
  @ApiOperation(value = "accepts a webhook request", nickname = "handleWebhookRequest")
  public void handleWebhookRequest(@PathParam("token") String token, PagerDutyIncidentDTO payload) {
    webhookService.handlePagerDutyWebhook(token, payload);
  }
}
