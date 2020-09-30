package io.harness.app.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.app.intfc.CIPipelineService;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.core.ci.services.BuildNumberService;
import io.harness.core.trigger.WebhookTriggerProcessor;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
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
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CIWebhookTriggerResource {
  private CIPipelineService ciPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;
  @Inject private SCMGrpc.SCMBlockingStub scmBlockingStub;
  private WebhookTriggerProcessor webhookTriggerProcessor;
  private BuildNumberService buildNumberService;

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
  @Path("/webhook/trigger/{id}")
  public RestResponse<String> runPipelineFromTrigger(
      @PathParam("id") String pipelineId, String eventPayload, @Context HttpHeaders httpHeaders) {
    try {
      NgPipelineEntity ngPipelineEntity = ciPipelineService.readPipeline(pipelineId);
      BuildNumber buildNumber = buildNumberService.increaseBuildNumber(ngPipelineEntity.getAccountId(),
          ngPipelineEntity.getOrgIdentifier(), ngPipelineEntity.getProjectIdentifier());
      CIExecutionArgs ciExecutionArgs =
          webhookTriggerProcessor.generateExecutionArgs(pipelineId, eventPayload, httpHeaders, buildNumber);
      ciPipelineExecutionService.executePipeline(ngPipelineEntity, ciExecutionArgs, buildNumber.getBuildNumber());
    } catch (Exception e) {
      logger.error("Failed to run input pipeline ", e);
    }

    return null;
  }
}
