package io.harness.ng.pipeline.resources;

import static io.harness.utils.PageUtils.getPageRequest;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineDTO;
import io.harness.cdng.pipeline.service.NgPipelineExecutionService;
import io.harness.cdng.pipeline.service.PipelineService;
import io.harness.execution.PlanExecution;
import io.harness.ng.core.ResponseDTO;
import io.harness.ng.core.RestQueryFilterParser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("pipelines")
@Path("pipelines")
@Produces(APPLICATION_JSON)
@Slf4j
public class CDNGPipelineResource {
  private PipelineService ngPipelineService;
  private final RestQueryFilterParser restQueryFilterParser;
  private final NgPipelineExecutionService ngPipelineExecutionService;

  private static final EmbeddedUser EMBEDDED_USER =
      EmbeddedUser.builder().uuid("lv0euRhKRCyiXWzS7pOg6g").email("admin@harness.io").name("Admin").build();

  @Inject
  public CDNGPipelineResource(PipelineService ngPipelineService, NgPipelineExecutionService ngPipelineExecutionService,
      RestQueryFilterParser restQueryFilterParser) {
    this.ngPipelineService = ngPipelineService;
    this.restQueryFilterParser = restQueryFilterParser;
    this.ngPipelineExecutionService = ngPipelineExecutionService;
  }

  @GET
  @Path("/{pipelineIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets a pipeline by identifier", nickname = "getPipeline")
  public ResponseDTO<CDPipelineDTO> getPipelineByIdentifier(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @PathParam("pipelineIdentifier") String pipelineId) {
    logger.info("Get pipeline");
    return ResponseDTO.newResponse(ngPipelineService.getPipeline(pipelineId, accountId, orgId, projectId));
  }

  @GET
  @Path("/{pipelineIdentifier}")
  @Produces({"text/yaml"})
  @Consumes({"text/yaml"})
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets a pipeline by identifier", nickname = "getPipelineYaml")
  public ResponseDTO<String> getNgPipelineByIdentifier(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @PathParam("pipelineIdentifier") String pipelineId) {
    logger.info("Get pipeline");
    return ResponseDTO.newResponse(
        ngPipelineService.getPipeline(pipelineId, accountId, orgId, projectId).getYamlPipeline());
  }

  // In case UI team fails to parse yaml from above API due to Response DTO wrapper, we will suggest below temporary API
  // for demo use
  @GET
  @Path("/yaml/{pipelineIdentifier}")
  @Produces({"text/yaml"})
  @Consumes({"text/yaml"})
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets a pipeline by identifier", nickname = "getPipelineYamlString")
  public String getNgPipelineByIdentifierYaml(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @PathParam("pipelineIdentifier") String pipelineId) {
    logger.info("Get pipeline");
    return ngPipelineService.getPipeline(pipelineId, accountId, orgId, projectId).getYamlPipeline();
  }

  @GET
  @Produces({APPLICATION_JSON, "text/yaml"})
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Pipeline list", nickname = "getPipelineList")
  public ResponseDTO<Page<CDPipelineDTO>> getListOfPipelines(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @NotNull @QueryParam("projectIdentifier") String projectId,
      @QueryParam("filter") String filterQuery, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("25") int size, @QueryParam("sort") List<String> sort) {
    logger.info("Get List of pipelines");
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, CDPipeline.class);
    Page<CDPipelineDTO> pipelines =
        ngPipelineService.getPipelines(accountId, orgId, projectId, criteria, getPageRequest(page, size, sort));
    return ResponseDTO.newResponse(pipelines);
  }

  @POST
  @Consumes({TEXT_PLAIN, "text/yaml"})
  @Produces({TEXT_PLAIN, "text/yaml"})
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create a Pipeline", nickname = "postPipeline")
  public ResponseDTO<String> createPipeline(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @NotNull @QueryParam("projectIdentifier") String projectId,
      @NotNull String yaml) {
    logger.info("Creating pipeline");
    return ResponseDTO.newResponse(ngPipelineService.createPipeline(yaml, accountId, orgId, projectId));
  }

  @PUT
  @Consumes({TEXT_PLAIN, "text/yaml"})
  @Produces({TEXT_PLAIN, "text/yaml"})
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Update a Pipeline", nickname = "putPipeline")
  public ResponseDTO<String> updatePipeline(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @NotNull @QueryParam("projectIdentifier") String projectId,
      @NotNull String yaml) {
    logger.info("Creating pipeline");
    return ResponseDTO.newResponse(ngPipelineService.updatePipeline(yaml, accountId, orgId, projectId));
  }

  @POST
  @Produces({"application/json", "text/html"})
  @Consumes({"application/json", "text/html"})
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create a Pipeline", nickname = "postPipelineDummy")
  public ResponseDTO<CDPipelineDTO> dummyCreatePipelineForSwagger(
      @NotNull @QueryParam("accountIdentifier") String accountId, @QueryParam("orgIdentifier") String orgId,
      @NotNull @QueryParam("projectIdentifier") String projectId, @NotNull @Valid CDPipelineDTO yaml) {
    logger.info("Creating pipeline");
    return ResponseDTO.newResponse(yaml);
  }

  @POST
  @Path("/{identifier}/execute")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Execute a pipeline", nickname = "postPipelineExecute")
  public ResponseDTO<PlanExecution> runPipeline(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @QueryParam("appId") String appId, @PathParam("identifier") @NotEmpty String pipelineId) {
    CDPipelineDTO cdPipelineDTO = ngPipelineService.getPipeline(pipelineId, accountId, orgId, projectId);
    // TODO: remove APPID once the dependency is moved.
    return ResponseDTO.newResponse(ngPipelineExecutionService.triggerPipeline(
        cdPipelineDTO.getYamlPipeline(), accountId, orgId, projectId, EMBEDDED_USER));
  }
}
