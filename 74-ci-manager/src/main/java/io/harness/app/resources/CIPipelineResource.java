package io.harness.app.resources;

import com.google.inject.Inject;

import io.harness.app.intfc.CIPipelineService;
import io.harness.app.yaml.YAML;
import io.harness.beans.CIPipeline;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.rest.RestResponse;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/ci")
@Produces(MediaType.APPLICATION_JSON)
public class CIPipelineResource {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CIPipelineResource.class);
  private CIPipelineService ciPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;

  @Inject
  public CIPipelineResource(
      CIPipelineService ciPipelineService, CIPipelineExecutionService ciPipelineExecutionService) {
    this.ciPipelineService = ciPipelineService;
    this.ciPipelineExecutionService = ciPipelineExecutionService;
  }

  @POST
  @Path("/pipelines")
  public RestResponse<String> createPipeline(String yaml) {
    logger.info("Creating pipeline");
    CIPipeline ciPipeline = ciPipelineService.createPipelineFromYAML(YAML.builder().pipelineYAML(yaml).build());
    return new RestResponse<>(ciPipeline.getUuid());
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
