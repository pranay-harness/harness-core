/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessModule._815_CG_TRIGGERS;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.WebHookRequest;
import software.wings.security.annotations.ApiKeyAuthorized;
import software.wings.service.intfc.WebHookService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Api("webhooks")
@Path("/webhooks")
@Produces("application/json")
@OwnedBy(CDC)
@TargetModule(_815_CG_TRIGGERS)
@ApiKeyAuthorized(allowEmptyApiKey = true, skipAuth = true)
public class WebHookResource {
  @Inject private WebHookService webHookService;

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
  public Response execute(
      @Context HttpHeaders httpHeaders, @PathParam("webHookToken") String webHookToken, WebHookRequest webHookRequest) {
    return webHookService.execute(webHookToken, webHookRequest, httpHeaders);
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
