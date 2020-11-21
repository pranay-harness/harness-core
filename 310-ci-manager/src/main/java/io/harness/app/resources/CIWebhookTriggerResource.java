package io.harness.app.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.core.ci.services.BuildNumberService;
import io.harness.core.trigger.WebhookTriggerProcessor;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.rest.RestResponse;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api("ci")
@Path("/ci")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CIWebhookTriggerResource {
  private NGPipelineService ngPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;
  @Inject private SCMGrpc.SCMBlockingStub scmBlockingStub;
  private WebhookTriggerProcessor webhookTriggerProcessor;
  private BuildNumberService buildNumberService;

  @POST
  @Consumes(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @Path("/webhook/trigger/{id}")
  public RestResponse<String> runPipelineFromTrigger(
      @PathParam("id") String pipelineId, String eventPayload, @Context HttpHeaders httpHeaders) {
    try {
      log.info("Received webhook for pipelineId {}", pipelineId);
      NgPipelineEntity ngPipelineEntity = ngPipelineService.getPipeline(pipelineId);
      BuildNumberDetails buildNumberDetails = buildNumberService.increaseBuildNumber(ngPipelineEntity.getAccountId(),
          ngPipelineEntity.getOrgIdentifier(), ngPipelineEntity.getProjectIdentifier());
      CIExecutionArgs ciExecutionArgs =
          webhookTriggerProcessor.generateExecutionArgs(pipelineId, eventPayload, httpHeaders, buildNumberDetails);
      ciPipelineExecutionService.executePipeline(
          ngPipelineEntity, ciExecutionArgs, buildNumberDetails.getBuildNumber());
    } catch (Exception e) {
      log.error("Failed to run input pipeline ", e);
    }

    return null;
  }
}
