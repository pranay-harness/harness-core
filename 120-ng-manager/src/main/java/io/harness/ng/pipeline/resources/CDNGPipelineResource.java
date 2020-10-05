package io.harness.ng.pipeline.resources;

import static io.harness.utils.PageUtils.getPageRequest;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.NGCommonEntityConstants;
import io.harness.NGConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStrategyType;
import io.harness.cdng.inputset.beans.resource.MergeInputSetRequestDTO;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.StepCategory;
import io.harness.cdng.pipeline.beans.CDPipelineValidationInfo;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineValidationInfoDTO;
import io.harness.cdng.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.cdng.pipeline.mappers.PipelineValidationMapper;
import io.harness.cdng.pipeline.service.PipelineService;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.ng.core.RestQueryFilterParser;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class CDNGPipelineResource {
  private final PipelineService ngPipelineService;
  private final RestQueryFilterParser restQueryFilterParser;
  private final NgPipelineExecutionService ngPipelineExecutionService;

  private static final EmbeddedUser EMBEDDED_USER =
      EmbeddedUser.builder().uuid("lv0euRhKRCyiXWzS7pOg6g").email("admin@harness.io").name("Admin").build();

  @GET
  @Path("/{pipelineIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets a pipeline by identifier", nickname = "getPipeline")
  public ResponseDTO<CDPipelineResponseDTO> getPipelineByIdentifier(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGConstants.PIPELINE_KEY) String pipelineId) {
    logger.info("Get pipeline");
    return ResponseDTO.newResponse(ngPipelineService.getPipeline(pipelineId, accountId, orgId, projectId).orElse(null));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Pipeline list", nickname = "getPipelineList")
  public ResponseDTO<Page<CDPipelineSummaryResponseDTO>> getListOfPipelines(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @QueryParam("filter") String filterQuery, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("25") int size, @QueryParam("sort") List<String> sort,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    logger.info("Get List of pipelines");
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, NgPipeline.class);
    Page<CDPipelineSummaryResponseDTO> pipelines = ngPipelineService.getPipelines(
        accountId, orgId, projectId, criteria, getPageRequest(page, size, sort), searchTerm);
    return ResponseDTO.newResponse(pipelines);
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiImplicitParams({
    @ApiImplicitParam(
        dataTypeClass = NgPipeline.class, dataType = "io.harness.cdng.pipeline.NgPipeline", paramType = "body")
  })
  @ApiOperation(value = "Create a Pipeline", nickname = "postPipeline")
  public ResponseDTO<String>
  createPipeline(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    logger.info("Creating pipeline");
    return ResponseDTO.newResponse(ngPipelineService.createPipeline(yaml, accountId, orgId, projectId));
  }

  @PUT
  @Path("/{pipelineIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiImplicitParams({
    @ApiImplicitParam(
        dataTypeClass = NgPipeline.class, dataType = "io.harness.cdng.pipeline.NgPipeline", paramType = "body")
  })
  @ApiOperation(value = "Update a Pipeline", nickname = "putPipeline")
  public ResponseDTO<String>
  updatePipeline(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGConstants.PIPELINE_KEY) String pipelineId,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    logger.info("Updating pipeline");
    return ResponseDTO.newResponse(ngPipelineService.updatePipeline(yaml, accountId, orgId, projectId, pipelineId));
  }

  @GET
  @Path("/validate")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Validate a Pipeline", nickname = "validatePipeline")
  public ResponseDTO<CDPipelineValidationInfoDTO> validatePipeline(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @QueryParam(NGConstants.PIPELINE_KEY) String pipelineId) throws IOException {
    logger.info("Validating pipeline");
    CDPipelineValidationInfo cdPipelineValidationInfo =
        ngPipelineService.validatePipeline(pipelineId, accountId, orgId, projectId).orElse(null);
    if (cdPipelineValidationInfo == null) {
      return ResponseDTO.newResponse(null);
    }
    return ResponseDTO.newResponse(PipelineValidationMapper.writePipelineValidationDto(cdPipelineValidationInfo));
  }

  @POST
  @Path("/execute/{identifier}")
  @Timed
  @ExceptionMetered
  @ApiImplicitParams({
    @ApiImplicitParam(
        dataTypeClass = NgPipeline.class, dataType = "io.harness.cdng.pipeline.CDPipeline", paramType = "body")
  })
  @ApiOperation(
      value = "Execute a pipeline with inputSet pipeline yaml", nickname = "postPipelineExecuteWithInputSetYaml")
  public ResponseDTO<NGPipelineExecutionResponseDTO>
  runPipelineWithInputSetPipelineYaml(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam("identifier") @NotEmpty String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @ApiParam(hidden = true, type = "") String inputSetPipelineYaml) {
    return ResponseDTO.newResponse(
        ngPipelineExecutionService.runPipelineWithInputSetPipelineYaml(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, inputSetPipelineYaml, useFQNIfErrorResponse, EMBEDDED_USER));
  }

  @POST
  @Path("/execute/{identifier}/inputSetList")
  @Timed
  @ExceptionMetered
  @ApiOperation(
      value = "Execute a pipeline with input set references list", nickname = "postPipelineExecuteWithInputSetList")
  public ResponseDTO<NGPipelineExecutionResponseDTO>
  runPipelineWithInputSetIdentifierList(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam("identifier") @NotEmpty String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTO mergeInputSetRequestDTO) {
    return ResponseDTO.newResponse(
        ngPipelineExecutionService.runPipelineWithInputSetReferencesList(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, mergeInputSetRequestDTO.getInputSetReferences(), useFQNIfErrorResponse, EMBEDDED_USER));
  }

  @GET
  @Path("/strategies")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Execution Strategy list", nickname = "getExecutionStrategyList")
  public ResponseDTO<Map<ServiceDefinitionType, List<ExecutionStrategyType>>> getExecutionStrategyList() {
    logger.info("Get List of execution Strategy");
    return ResponseDTO.newResponse(ngPipelineService.getExecutionStrategyList());
  }

  @GET
  @Path("/strategies/yaml-snippets")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Yaml for Execution Strategy based on deployment type and selected strategy",
      nickname = "getExecutionStrategyYaml")
  public ResponseDTO<String>
  getExecutionStrategyYaml(@NotNull @QueryParam("serviceDefinitionType") ServiceDefinitionType serviceDefinitionType,
      @NotNull @QueryParam("strategyType") ExecutionStrategyType executionStrategyType) throws IOException {
    return ResponseDTO.newResponse(
        ngPipelineService.getExecutionStrategyYaml(serviceDefinitionType, executionStrategyType));
  }

  @GET
  @Path("/serviceDefinitionTypes")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Git list of service definition types", nickname = "getServiceDefinitionTypes")
  public ResponseDTO<List<ServiceDefinitionType>> getServiceDefinitionTypes() {
    return ResponseDTO.newResponse(ngPipelineService.getServiceDefinitionTypes());
  }

  @GET
  @Path("/steps")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get steps for given service definition type", nickname = "getSteps")
  public ResponseDTO<StepCategory> getSteps(
      @NotNull @QueryParam("serviceDefinitionType") ServiceDefinitionType serviceDefinitionType) {
    return ResponseDTO.newResponse(ngPipelineService.getSteps(serviceDefinitionType));
  }

  @DELETE
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Delete a pipeline", nickname = "softDeletePipeline")
  public ResponseDTO<Boolean> deletePipeline(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGConstants.PIPELINE_KEY) String pipelineId) {
    return ResponseDTO.newResponse(ngPipelineService.deletePipeline(accountId, orgId, projectId, pipelineId));
  }
}
