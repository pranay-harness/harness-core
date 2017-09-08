package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.WebHookService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
  public RestResponse<WebHookResponse> execute(
      @PathParam("webHookToken") String webHookToken, WebHookRequest webHookRequest) {
    return new RestResponse<>(webHookService.execute(webHookToken, webHookRequest));
  }
}
