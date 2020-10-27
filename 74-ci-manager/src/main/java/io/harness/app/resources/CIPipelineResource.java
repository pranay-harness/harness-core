package io.harness.app.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.core.ci.services.BuildNumberService;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.annotations.NextGenManagerAuth;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("ci")
@Path("/ci")
@NextGenManagerAuth
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CIPipelineResource {
  private NGPipelineService ngPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;
  private BuildNumberService buildNumberService;

  @POST
  @Path("/execute/{identifier}/run")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Execute a CI pipeline", nickname = "executePipeline")
  public RestResponse<String> runPipeline(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @PathParam("identifier") @NotEmpty String pipelineId) {
    try {
      NgPipelineEntity ngPipelineEntity = ngPipelineService.getPipeline(pipelineId, accountId, orgId, projectId);
      BuildNumber buildNumber = buildNumberService.increaseBuildNumber(accountId, orgId, projectId);

      // TODO create manual execution source
      CIExecutionArgs ciExecutionArgs = CIExecutionArgs.builder().buildNumber(buildNumber).build();
      ciPipelineExecutionService.executePipeline(ngPipelineEntity, ciExecutionArgs, 1L);
    } catch (Exception e) {
      logger.error("Failed to run input pipeline ", e);
    }

    return null;
  }
}
