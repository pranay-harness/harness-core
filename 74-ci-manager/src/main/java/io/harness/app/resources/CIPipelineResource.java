package io.harness.app.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.yaml.YAML;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.core.ci.services.BuildNumberService;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("ci")
@Path("/ci")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CIPipelineResource {
  private CIPipelineService ciPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;
  private BuildNumberService buildNumberService;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create a CI Pipeline", nickname = "postPipeline")
  @Path("/pipelines")
  public RestResponse<String> createPipeline(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @NotNull @QueryParam("projectIdentifier") String projectId,
      @NotNull String yaml) {
    logger.info("Creating pipeline");
    NgPipelineEntity ngPipelineEntity = ciPipelineService.createPipelineFromYAML(
        YAML.builder().pipelineYAML(yaml).build(), accountId, orgId, projectId);
    return new RestResponse<>(ngPipelineEntity.getUuid());
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets a CI pipeline by identifier", nickname = "getPipeline")
  @Path("/pipelines/{pipelineIdentifier}")
  public ResponseDTO<NgPipelineEntity> getPipelineByIdentifier(
      @NotNull @QueryParam("accountIdentifier") String accountId, @QueryParam("orgIdentifier") String orgId,
      @QueryParam("projectIdentifier") String projectId, @PathParam("pipelineIdentifier") String pipelineId) {
    logger.info("Fetching pipeline");
    return ResponseDTO.newResponse(ciPipelineService.readPipeline(pipelineId, accountId, orgId, projectId));
  }

  @POST
  @Path("/pipelines/{identifier}/run")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Execute a CI pipeline", nickname = "executePipeline")
  public RestResponse<String> runPipeline(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @PathParam("identifier") @NotEmpty String pipelineId) {
    try {
      NgPipelineEntity ngPipelineEntity = ciPipelineService.readPipeline(pipelineId, accountId, orgId, projectId);
      BuildNumber buildNumber = buildNumberService.increaseBuildNumber(ngPipelineEntity.getAccountId(),
          ngPipelineEntity.getOrgIdentifier(), ngPipelineEntity.getProjectIdentifier());
      // TODO create manual execution source
      CIExecutionArgs ciExecutionArgs = CIExecutionArgs.builder().buildNumber(buildNumber).build();
      ciPipelineExecutionService.executePipeline(ngPipelineEntity, ciExecutionArgs, 1L);
    } catch (Exception e) {
      logger.error("Failed to run input pipeline ", e);
    }

    return null;
  }
}
