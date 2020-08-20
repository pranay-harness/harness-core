package io.harness.app.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.app.intfc.CIPipelineService;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WebHookRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Api("ci")
@Path("/ci")
@Produces("application/json")
@PublicApi
@Slf4j
public class CIWebhookTriggerResource {
  private CIPipelineService ciPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;

  @GET
  @Timed
  @ExceptionMetered
  @Path("{id}")
  public Response ping(@PathParam("id") String webHookToken, WebHookRequest webHookRequest) {
    return Response.status(Response.Status.OK).build();
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @Path("/pipelines/{id}/trigger")
  public RestResponse<String> runPipelineFromTrigger(
      @PathParam("id") String pipelineId, String eventPayload, @Context HttpHeaders httpHeaders) {
    try {
      ciPipelineExecutionService.executePipeline(ciPipelineService.readPipeline(pipelineId));
    } catch (Exception e) {
      logger.error("Failed to run input pipeline ", e);
    }

    return null;
  }
}
