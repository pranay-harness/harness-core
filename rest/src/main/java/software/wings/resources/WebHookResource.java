package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.WebHookService;
import software.wings.utils.Validator;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

@Api("webhooks")
@Path("/webhooks")
@Produces("application/json")
@PublicApi
public class WebHookResource {
  @Inject private WebHookService webHookService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("{webHookToken}")
  public WebHookResponse execute(@PathParam("webHookToken") String webHookToken, WebHookRequest webHookRequest) {
    Validator.notNullCheck("Request body", webHookRequest);
    return webHookService.execute(webHookToken, webHookRequest);
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("{webHookToken}/git")
  public WebHookResponse executeGit(
      @PathParam("webHookToken") String webHookToken, String eventPayload, @Context HttpHeaders httpHeaders) {
    return webHookService.executeByEvent(webHookToken, eventPayload, httpHeaders);
  }
}
