package io.harness.pms.pipeline;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.filter.creation.PMSPipelineFilterRequestDTO;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.ExecutionGraphMapper;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.pms.pipeline.mappers.PipelineExecutionSummaryDtoMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionDetailDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionFilterPropertiesDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.variables.VariableMergeServiceResponse;
import io.harness.serializer.JsonUtils;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.*;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

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
public class PipelineResource {
  private final PMSPipelineService pmsPipelineService;
  private final PMSExecutionService pmsExecutionService;

  @POST
  @ApiOperation(value = "Create a Pipeline", nickname = "createPipeline")
  public ResponseDTO<String> createPipeline(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @ApiParam(hidden = true) String yaml) {
    log.info("Creating pipeline");

    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    PipelineEntity createdEntity = pmsPipelineService.create(pipelineEntity);

    return ResponseDTO.newResponse(createdEntity.getVersion().toString(), createdEntity.getIdentifier());
  }

  @POST
  @Path("/{pipelineIdentifier}/variables")
  @ApiOperation(value = "Create variables for Pipeline", nickname = "createVariables")
  public ResponseDTO<VariableMergeServiceResponse> createVariables(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId,
      @NotNull @ApiParam(hidden = true) String yaml) {
    log.info("Creating variables for pipeline.");

    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    VariableMergeServiceResponse variablesResponse = pmsPipelineService.createVariablesResponse(pipelineEntity);

    return ResponseDTO.newResponse(variablesResponse);
  }

  @GET
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Gets a pipeline by identifier", nickname = "getPipeline")
  public ResponseDTO<PMSPipelineResponseDTO> getPipelineByIdentifier(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId) {
    log.info("Get pipeline");

    Optional<PipelineEntity> pipelineEntity = pmsPipelineService.get(accountId, orgId, projectId, pipelineId, false);
    String version = "0";
    if (pipelineEntity.isPresent()) {
      version = pipelineEntity.get().getVersion().toString();
    }
    return ResponseDTO.newResponse(version, pipelineEntity.map(PMSPipelineDtoMapper::writePipelineDto).orElse(null));
  }

  @PUT
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Update a Pipeline", nickname = "putPipeline")
  public ResponseDTO<String> updatePipeline(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId,
      @NotNull @ApiParam(hidden = true) String yaml) {
    log.info("Updating pipeline");

    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    if (!pipelineEntity.getIdentifier().equals(pipelineId)) {
      throw new InvalidRequestException("Pipeline identifier in URL does not match pipeline identifier in yaml");
    }

    pipelineEntity.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    PipelineEntity updatedEntity = pmsPipelineService.update(pipelineEntity);

    return ResponseDTO.newResponse(updatedEntity.getVersion().toString(), updatedEntity.getIdentifier());
  }

  @DELETE
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Delete a pipeline", nickname = "softDeletePipeline")
  public ResponseDTO<Boolean> deletePipeline(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId) {
    log.info("Deleting pipeline");

    return ResponseDTO.newResponse(pmsPipelineService.delete(
        accountId, orgId, projectId, pipelineId, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @GET
  @ApiOperation(value = "Gets Pipeline list", nickname = "getPipelineList")
  public ResponseDTO<Page<PMSPipelineSummaryResponseDTO>> getListOfPipelines(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @QueryParam("filter") String filterQuery, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("25") int size, @QueryParam("sort") List<String> sort,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @QueryParam("module") String module) {
    log.info("Get List of pipelines");
    PMSPipelineFilterRequestDTO pmsPipelineFilterRequestDTO = null;

    if (EmptyPredicate.isNotEmpty(filterQuery)) {
      pmsPipelineFilterRequestDTO = JsonUtils.asObject(filterQuery, PMSPipelineFilterRequestDTO.class);
    }
    Criteria criteria = PMSPipelineFilterHelper.createCriteriaForGetList(
        accountId, orgId, projectId, pmsPipelineFilterRequestDTO, module, searchTerm, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.lastUpdatedAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    Page<PMSPipelineSummaryResponseDTO> pipelines =
        pmsPipelineService.list(criteria, pageRequest).map(PMSPipelineDtoMapper::preparePipelineSummary);

    return ResponseDTO.newResponse(pipelines);
  }

  @GET
  @Path("/summary/{pipelineIdentifier}")
  @ApiOperation(value = "Gets Pipeline Summary of a pipeline", nickname = "getPipelineSummary")
  public ResponseDTO<PMSPipelineSummaryResponseDTO> getPipelineSummary(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId) {
    log.info("Get pipeline Summary");

    PMSPipelineSummaryResponseDTO pipelineSummary = PMSPipelineDtoMapper.preparePipelineSummary(
        pmsPipelineService.get(accountId, orgId, projectId, pipelineId, false)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 String.format("Pipeline with the given ID: %s does not exisit", pipelineId))));

    return ResponseDTO.newResponse(pipelineSummary);
  }

  @GET
  @Path("/steps")
  @ApiOperation(value = "Get Steps for given module", nickname = "getSteps")
  public ResponseDTO<StepCategory> getSteps(
      @NotNull @QueryParam("category") String category, @NotNull @QueryParam("module") String module) {
    log.info("Get Steps for given module");

    return ResponseDTO.newResponse(pmsPipelineService.getSteps(module, category));
  }

  @POST
  @Path("/execution/summary")
  @ApiOperation(value = "Gets Executions list", nickname = "getListOfExecutions")
  public ResponseDTO<Page<PipelineExecutionSummaryDTO>> getListOfExecutions(
      @NotNull @QueryParam("accountIdentifier") String accountId, @QueryParam("orgIdentifier") String orgId,
      @NotNull @QueryParam("projectIdentifier") String projectId, @QueryParam("filter") String filter,
      @QueryParam("pipelineIdentifier") String pipelineIdentifier, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("10") int size, @QueryParam("sort") List<String> sort,
      @QueryParam("filterIdentifier") String filterIdentifier, FilterPropertiesDTO filterProperties) {
    log.info("Get List of executions");
    Criteria criteria = pmsExecutionService.formCriteria(accountId, orgId, projectId, pipelineIdentifier,
        filterIdentifier, (PipelineExecutionFilterPropertiesDTO) filterProperties);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    Page<PipelineExecutionSummaryDTO> planExecutionSummaryDTOS =
        pmsExecutionService.getPipelineExecutionSummaryEntity(criteria, pageRequest)
            .map(PipelineExecutionSummaryDtoMapper::toDto);

    return ResponseDTO.newResponse(planExecutionSummaryDTOS);
  }

  @GET
  @Path("/execution/{planExecutionId}")
  @ApiOperation(value = "Gets Execution Detail", nickname = "getExecutionDetail")
  public ResponseDTO<PipelineExecutionDetailDTO> getExecutionDetail(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId, @QueryParam("filter") String filter,
      @QueryParam("stageNodeId") String stageNodeId,
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    log.info("Get Execution Detail");

    PipelineExecutionDetailDTO pipelineExecutionDetailDTO =
        PipelineExecutionDetailDTO.builder()
            .pipelineExecutionSummary(PipelineExecutionSummaryDtoMapper.toDto(
                pmsExecutionService.getPipelineExecutionSummaryEntity(accountId, orgId, projectId, planExecutionId)))
            .executionGraph(ExecutionGraphMapper.toExecutionGraph(
                pmsExecutionService.getOrchestrationGraph(stageNodeId, planExecutionId)))
            .build();

    return ResponseDTO.newResponse(pipelineExecutionDetailDTO);
  }

  @GET
  @Path("/execution/{planExecutionId}/inputset")
  @ApiOperation(value = "Gets  inputsetYaml", nickname = "getInputsetYaml")
  public String getInputsetYaml(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    return pmsExecutionService.getInputsetYaml(accountId, orgId, projectId, planExecutionId);
  }
}
