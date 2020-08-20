package io.harness.app.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.yaml.YAML;
import io.harness.beans.CIPipeline;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("ci")
@Path("/ci")
@Produces("application/json")
@PublicApi
@Slf4j
public class CIPipelineResource {
  private CIPipelineService ciPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;

  @Inject
  public CIPipelineResource(
      CIPipelineService ciPipelineService, CIPipelineExecutionService ciPipelineExecutionService) {
    this.ciPipelineService = ciPipelineService;
    this.ciPipelineExecutionService = ciPipelineExecutionService;
  }
  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create a CI Pipeline", nickname = "postPipeline")
  @Path("/pipelines")
  public RestResponse<String> createPipeline(String yaml) {
    logger.info("Creating pipeline");
    CIPipeline ciPipeline = ciPipelineService.createPipelineFromYAML(YAML.builder().pipelineYAML(yaml).build());
    return new RestResponse<>(ciPipeline.getUuid());
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets a CI pipeline by identifier", nickname = "getPipeline")
  @Path("/pipelines/{pipelineIdentifier}")
  public ResponseDTO<CIPipeline> getPipelineByIdentifier(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @PathParam("pipelineIdentifier") String pipelineId) {
    logger.info("Fetching pipeline");
    return ResponseDTO.newResponse(ciPipelineService.readPipeline(pipelineId));
  }

  @POST
  @Path("/pipelines/{id}/run")
  public RestResponse<String> runPipeline(@PathParam("id") @NotEmpty String pipelineId) {
    try {
      ciPipelineExecutionService.executePipeline(ciPipelineService.readPipeline(pipelineId));
    } catch (Exception e) {
      logger.error("Failed to run input pipeline ", e);
    }

    return null;
  }
}
