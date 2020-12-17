package io.harness.pms.pipeline;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.exception.service.PMSExecutionService;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.beans.ExecutionGraph;
import io.harness.pms.execution.beans.ExecutionNode;
import io.harness.pms.execution.beans.ExecutionNodeAdjacencyList;
import io.harness.pms.filter.creation.PMSPipelineFilterRequestDTO;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.pms.pipeline.resource.PipelineExecutionDetailDTO;
import io.harness.pms.pipeline.resource.PipelineExecutionSummaryDTO;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineServiceImpl;
import io.harness.serializer.JsonUtils;
import io.harness.tasks.ProgressData;
import io.harness.utils.PageUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.groovy.util.Maps;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
  private PMSPipelineService pmsPipelineService;
  private PMSExecutionService pmsExecutionService;

  @POST
  @ApiOperation(value = "Create a Pipeline", nickname = "createPipeline")
  public ResponseDTO createPipeline(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) throws IOException {
    log.info("Creating pipeline");

    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    PipelineEntity createdEntity = pmsPipelineService.create(pipelineEntity);

    return ResponseDTO.newResponse(createdEntity.getVersion().toString(), createdEntity.getIdentifier());
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

    return ResponseDTO.newResponse(pipelineEntity.get().getVersion().toString(),
        pipelineEntity.map(PMSPipelineDtoMapper::writePipelineDto).orElse(null));
  }

  @PUT
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Update a Pipeline", nickname = "putPipeline")
  public ResponseDTO<String> updatePipeline(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
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
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.createdAt));
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

  @GET
  @Path("/execution/summary")
  @ApiOperation(value = "Gets Executions list", nickname = "getListOfExecutions")
  public ResponseDTO<Page<PipelineExecutionSummaryDTO>> getListOfExecutions(
      @NotNull @QueryParam("accountIdentifier") String accountId, @QueryParam("orgIdentifier") String orgId,
      @NotNull @QueryParam("projectIdentifier") String projectId, @QueryParam("filter") String filter,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("sort") List<String> sort) {
    log.info("Get List of executions");
    List<PipelineExecutionSummaryDTO> planExecutionSummaryDTOS = new ArrayList<>();
    planExecutionSummaryDTOS.add(
        JsonUtils.asObject(PMSPipelineServiceImpl.PIPELINE_EXECUTION_SUMMARY_JSON, PipelineExecutionSummaryDTO.class));

    return ResponseDTO.newResponse(new PageImpl<>(
        planExecutionSummaryDTOS, PageUtils.getPageRequest(page, size, sort), planExecutionSummaryDTOS.size()));
  }

  @GET
  @Path("/execution/{planExecutionId}")
  @ApiOperation(value = "Gets Execution Detail", nickname = "getExecutionDetail")
  public ResponseDTO<PipelineExecutionDetailDTO> getExecutionDetail(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId, @QueryParam("filter") String filter,
      @QueryParam("stageIdentifier") String stageIdentifier,
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    log.info("Get Execution Detail");

    ExecutionGraph executionGraph = null;
    if (isNotEmpty(stageIdentifier)) {
      executionGraph = generateExecutionGraph(stageIdentifier);
    }

    PipelineExecutionDetailDTO pipelineExecutionDetailDTO =
        PipelineExecutionDetailDTO.builder()
            .pipelineExecutionSummary(JsonUtils.asObject(
                PMSPipelineServiceImpl.PIPELINE_EXECUTION_SUMMARY_JSON, PipelineExecutionSummaryDTO.class))
            .executionGraph(executionGraph)
            .build();

    return ResponseDTO.newResponse(pipelineExecutionDetailDTO);
  }

  @GET
  @Path("/execution/{planExecutionId}/inputset")
  @ApiOperation(value = "Gets  inputsetYaml", nickname = "getInputsetYaml")
  public String getInputsetYaml(@PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    return pmsExecutionService.getInputsetYaml(planExecutionId);
  }

  private ExecutionGraph generateExecutionGraph(String stageIdentifier) {
    String serviceUuid = "serviceUuid";
    String infraUuid = "infraUuid";
    String k8sRollingUuid = "K8sRollingUuid";

    Map<String, ExecutionNode> executionNodeMap = new HashMap<>();
    Map<String, ExecutionNodeAdjacencyList> nodeAdjacencyListMap = new HashMap<>();

    ExecutionNode qaStage = ExecutionNode.builder()
                                .uuid(stageIdentifier)
                                .name("qa stage")
                                .startTs(System.currentTimeMillis())
                                .status(ExecutionStatus.RUNNING)
                                .stepType("SECTION")
                                .build();
    executionNodeMap.put(stageIdentifier, qaStage);
    nodeAdjacencyListMap.put(
        stageIdentifier, ExecutionNodeAdjacencyList.builder().children(Lists.newArrayList(serviceUuid)).build());

    ExecutionNode service = ExecutionNode.builder()
                                .uuid(serviceUuid)
                                .name("Service")
                                .startTs(System.currentTimeMillis())
                                .endTs(System.currentTimeMillis())
                                .status(ExecutionStatus.SUCCESS)
                                .stepType("SERVICE")
                                .taskIdToProgressDataMap(Maps.of(generateUuid(),
                                    Arrays.asList(DummyProgressData.builder().data("50% done").build(),
                                        DummyProgressData.builder().data("99% done").build())))
                                .build();
    executionNodeMap.put(serviceUuid, service);
    nodeAdjacencyListMap.put(
        serviceUuid, ExecutionNodeAdjacencyList.builder().nextIds(Lists.newArrayList(infraUuid)).build());

    ExecutionNode infra = ExecutionNode.builder()
                              .uuid(infraUuid)
                              .name("Infrastructure")
                              .startTs(System.currentTimeMillis())
                              .endTs(System.currentTimeMillis())
                              .status(ExecutionStatus.SUCCESS)
                              .stepType("INFRASTRUCTURE")
                              .taskIdToProgressDataMap(Maps.of(generateUuid(),
                                  Arrays.asList(DummyProgressData.builder().data("33% done").build(),
                                      DummyProgressData.builder().data("99% done").build())))
                              .build();
    executionNodeMap.put(infraUuid, infra);
    nodeAdjacencyListMap.put(
        infraUuid, ExecutionNodeAdjacencyList.builder().nextIds(Lists.newArrayList(k8sRollingUuid)).build());

    org.bson.Document loggingMetadata =
        new org.bson.Document()
            .append("baseLoggingKey", "baseKey")
            .append("commandUnits",
                Arrays.asList("Fetch Files", "Initialize", "Prepare", "Apply", "Wait for Steady State", "Wrap Up"));

    ExecutionNode k8sRolling = ExecutionNode.builder()
                                   .uuid(k8sRollingUuid)
                                   .name("Rollout Deployment")
                                   .startTs(System.currentTimeMillis())
                                   .status(ExecutionStatus.RUNNING)
                                   .stepType("K8S_ROLLING")
                                   .executableResponsesMetadata(Lists.newArrayList(loggingMetadata))
                                   .taskIdToProgressDataMap(Maps.of(generateUuid(),
                                       Arrays.asList(DummyProgressData.builder().data("33% done").build(),
                                           DummyProgressData.builder().data("55% done").build())))
                                   .build();
    executionNodeMap.put(k8sRollingUuid, k8sRolling);
    nodeAdjacencyListMap.put(k8sRollingUuid, ExecutionNodeAdjacencyList.builder().build());

    return ExecutionGraph.builder()
        .rootNodeId(stageIdentifier)
        .nodeMap(executionNodeMap)
        .nodeAdjacencyListMap(nodeAdjacencyListMap)
        .build();
  }

  @Value
  @Builder
  private static class DummyProgressData implements ProgressData {
    String data;
  }
}
