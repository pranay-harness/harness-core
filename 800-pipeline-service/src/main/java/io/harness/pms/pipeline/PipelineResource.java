package io.harness.pms.pipeline;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.beans.ExecutionGraph;
import io.harness.pms.execution.beans.ExecutionNode;
import io.harness.pms.execution.beans.ExecutionNodeAdjacencyList;
import io.harness.pms.filter.creation.PMSPipelineFilterRequestDTO;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.pms.pipeline.resource.EdgeLayoutListDTO;
import io.harness.pms.pipeline.resource.GraphLayoutNodeDTO;
import io.harness.pms.pipeline.resource.PipelineExecutionDetailDTO;
import io.harness.pms.pipeline.resource.PipelineExecutionSummaryDTO;
import io.harness.pms.pipeline.service.PMSPipelineService;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

  // ToDo(Alexei) should be removed
  @Deprecated
  @GET
  @Path("/summary/execution/{pipelineIdentifier}")
  @ApiOperation(value = "Gets Plan Summary of a pipeline", nickname = "getPlanSummary")
  public ResponseDTO<PipelineExecutionSummaryDTO> getPlanSummary(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId) {
    log.info("Get plan Summary");
    return ResponseDTO.newResponse(generatePipelineExecutionSummaryDTO(null));
  }

  @GET
  @Path("/execution/summary")
  @ApiOperation(value = "Gets Executions list", nickname = "getListOfExecutions")
  public ResponseDTO<List<PipelineExecutionSummaryDTO>> getListOfExecutions(
      @NotNull @QueryParam("accountIdentifier") String accountId, @QueryParam("orgIdentifier") String orgId,
      @NotNull @QueryParam("projectIdentifier") String projectId, @QueryParam("filter") String filter,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("sort") List<String> sort) {
    log.info("Get List of executions");
    List<PipelineExecutionSummaryDTO> planExecutionSummaryDTOS = new ArrayList<>();
    planExecutionSummaryDTOS.add(generatePipelineExecutionSummaryDTO(null));
    planExecutionSummaryDTOS.add(generatePipelineExecutionSummaryDTO(null));
    return ResponseDTO.newResponse(planExecutionSummaryDTOS);
  }

  @GET
  @Path("/execution/{planExecutionId}")
  @ApiOperation(value = "Gets Execution Detail", nickname = "getExecutionDetail")
  public ResponseDTO<PipelineExecutionDetailDTO> getExecutionDetail(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @QueryParam("stageIdentifier") String stageIdentifier,
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    log.info("Get Execution Detail");

    ExecutionGraph executionGraph = null;
    if (isNotEmpty(stageIdentifier)) {
      executionGraph = generateExecutionGraph(stageIdentifier);
    }

    PipelineExecutionDetailDTO pipelineExecutionDetailDTO =
        PipelineExecutionDetailDTO.builder()
            .pipelineExecutionSummary(generatePipelineExecutionSummaryDTO(planExecutionId))
            .executionGraph(executionGraph)
            .build();

    return ResponseDTO.newResponse(pipelineExecutionDetailDTO);
  }

  private PipelineExecutionSummaryDTO generatePipelineExecutionSummaryDTO(String planExecutionId) {
    if (planExecutionId == null) {
      planExecutionId = generateUuid();
    }
    String pipelineNodeUuid = generateUuid();
    String stageNodeUuid = generateUuid();
    String ciStageUuid = generateUuid();
    String ciUnitTest1Uuid = generateUuid();
    String ciUnitTest2Uuid = generateUuid();
    String staging1Uuid = generateUuid();
    String cdStageUuid = generateUuid();
    String cdRollingDeploymentId = generateUuid();

    Map<String, GraphLayoutNodeDTO> layoutNodeMap = new HashMap<>();
    GraphLayoutNodeDTO pipelineLayoutNode =
        GraphLayoutNodeDTO.builder()
            .nodeType("pipeline")
            .nodeIdentifier("pipeline")
            .nodeUuid(pipelineNodeUuid)
            .status(ExecutionStatus.RUNNING)
            .edgeLayoutList(EdgeLayoutListDTO.builder().nextIds(Collections.singletonList(stageNodeUuid)).build())
            .build();

    GraphLayoutNodeDTO stageLayoutNode =
        GraphLayoutNodeDTO.builder()
            .nodeUuid(stageNodeUuid)
            .nodeType("stages")
            .nodeIdentifier("stages")
            .status(ExecutionStatus.RUNNING)
            .edgeLayoutList(EdgeLayoutListDTO.builder().nextIds(Lists.newArrayList(ciStageUuid)).build())
            .build();
    GraphLayoutNodeDTO ciStage =
        GraphLayoutNodeDTO.builder()
            .nodeUuid(ciStageUuid)
            .nodeType("ciStage")
            .nodeIdentifier("ciStage")
            .status(ExecutionStatus.RUNNING)
            .edgeLayoutList(EdgeLayoutListDTO.builder()
                                .currentNodeChildren(Lists.newArrayList(ciUnitTest1Uuid, ciUnitTest2Uuid))
                                .nextIds(Lists.newArrayList(staging1Uuid))
                                .build())
            .build();
    GraphLayoutNodeDTO ciTest1 = GraphLayoutNodeDTO.builder()
                                     .nodeUuid(ciUnitTest1Uuid)
                                     .nodeType("ci_unit_tests_1")
                                     .nodeIdentifier("ci_unit_tests_1")
                                     .status(ExecutionStatus.SUCCESS)
                                     .moduleInfo(Maps.of("CI",
                                         new org.bson.Document().append("unit-tests-1",
                                             new org.bson.Document()
                                                 .append("name", "unit-tests-1")
                                                 .append("numberOfTests", "115")
                                                 .append("numberOfFlakyTests", "0")
                                                 .append("status", "success"))))
                                     .edgeLayoutList(EdgeLayoutListDTO.builder().build())
                                     .build();
    GraphLayoutNodeDTO ciTest2 = GraphLayoutNodeDTO.builder()
                                     .nodeUuid(ciUnitTest2Uuid)
                                     .nodeType("ci_unit_tests_2")
                                     .nodeIdentifier("ci_unit_tests_2")
                                     .status(ExecutionStatus.SUSPENDED)
                                     .edgeLayoutList(EdgeLayoutListDTO.builder().build())
                                     .build();
    GraphLayoutNodeDTO staging1 =
        GraphLayoutNodeDTO.builder()
            .nodeUuid(staging1Uuid)
            .nodeType("Staging1")
            .nodeIdentifier("Staging1")
            .status(ExecutionStatus.RUNNING)
            .edgeLayoutList(EdgeLayoutListDTO.builder().nextIds(Lists.newArrayList(cdStageUuid)).build())
            .build();

    GraphLayoutNodeDTO cdStage =
        GraphLayoutNodeDTO.builder()
            .nodeUuid(cdStageUuid)
            .nodeType("cdStage")
            .nodeIdentifier("cdStage")
            .status(ExecutionStatus.RUNNING)
            .moduleInfo(Maps.of(
                "CD", new org.bson.Document().append("DeploymentType", "k8s").append("namespace", "mock-namespace")))
            .edgeLayoutList(EdgeLayoutListDTO.builder().nextIds(Lists.newArrayList(cdRollingDeploymentId)).build())
            .build();
    GraphLayoutNodeDTO cdRollingDeployment = GraphLayoutNodeDTO.builder()
                                                 .nodeUuid(cdRollingDeploymentId)
                                                 .nodeType("Rolling Deployment")
                                                 .nodeIdentifier("Rolling Deployments")
                                                 .status(ExecutionStatus.RUNNING)
                                                 .edgeLayoutList(EdgeLayoutListDTO.builder().build())
                                                 .build();
    layoutNodeMap.put(pipelineNodeUuid, pipelineLayoutNode);
    layoutNodeMap.put(stageNodeUuid, stageLayoutNode);
    layoutNodeMap.put(ciStageUuid, ciStage);
    layoutNodeMap.put(ciUnitTest1Uuid, ciTest1);
    layoutNodeMap.put(ciUnitTest2Uuid, ciTest2);
    layoutNodeMap.put(staging1Uuid, staging1);
    layoutNodeMap.put(cdStageUuid, cdStage);
    layoutNodeMap.put(cdRollingDeploymentId, cdRollingDeployment);

    Map<String, org.bson.Document> moduleInfo = new HashMap<>();
    moduleInfo.put("CI",
        new org.bson.Document().append("ciBuildBranchHook",
            new org.bson.Document()
                .append("name", "master")
                .append("link", "https://github.com/wings-software/portal/blob/master/")
                .append("state", "CLOSED")
                .append("commits", "[]")));
    moduleInfo.put("CD", new org.bson.Document().append("DeploymentType", "k8s").append("namespace", "mock-namespace"));

    return PipelineExecutionSummaryDTO.builder()
        .pipelineIdentifier(pipelineNodeUuid)
        .planExecutionId(planExecutionId)
        .name("Mock Pipeline")
        .createdAt(System.currentTimeMillis())
        .executionTriggerInfo(ExecutionTriggerInfo.builder()
                                  .triggeredBy(EmbeddedUser.builder().name("Harness Dev").build())
                                  .triggerType(TriggerType.MANUAL)
                                  .build())
        .startTs(System.currentTimeMillis())
        .endTs(System.currentTimeMillis() + 5000)
        .status(ExecutionStatus.RUNNING)
        .layoutNodeMap(layoutNodeMap)
        .startingNodeId(pipelineNodeUuid)
        .moduleInfo(moduleInfo)
        .build();
  }

  private ExecutionGraph generateExecutionGraph(String stageIdentifier) {
    String serviceUuid = generateUuid();
    String provisioningInfraUuid = generateUuid();
    String infraUuid = generateUuid();
    String terraformPlanUuid = generateUuid();
    String approvalUuid = generateUuid();
    String terraformApplyUuid = generateUuid();
    String executionUuid = generateUuid();
    String k8sUpdateUuid = generateUuid();
    String forkUuid = generateUuid();
    String step1Uuid = generateUuid();
    String step2Uuid = generateUuid();

    Map<String, ExecutionNode> executionNodeMap = new HashMap<>();
    Map<String, ExecutionNodeAdjacencyList> nodeAdjacencyListMap = new HashMap<>();

    ExecutionNode staging1 = ExecutionNode.builder()
                                 .uuid(stageIdentifier)
                                 .name("Staging1")
                                 .startTs(System.currentTimeMillis())
                                 .status(ExecutionStatus.RUNNING)
                                 .stepType("STAGE")
                                 .build();
    executionNodeMap.put(stageIdentifier, staging1);
    nodeAdjacencyListMap.put(
        stageIdentifier, ExecutionNodeAdjacencyList.builder().nextIds(Lists.newArrayList(serviceUuid)).build());

    ExecutionNode service = ExecutionNode.builder()
                                .uuid(serviceUuid)
                                .name("Service")
                                .startTs(System.currentTimeMillis())
                                .endTs(System.currentTimeMillis())
                                .status(ExecutionStatus.SUCCESS)
                                .stepType("SERVICE")
                                .build();
    executionNodeMap.put(serviceUuid, service);
    nodeAdjacencyListMap.put(
        serviceUuid, ExecutionNodeAdjacencyList.builder().nextIds(Lists.newArrayList(provisioningInfraUuid)).build());

    ExecutionNode provisioningInfra =
        ExecutionNode.builder()
            .uuid(provisioningInfraUuid)
            .name("Provisioning Infrastructure")
            .startTs(System.currentTimeMillis())
            .endTs(System.currentTimeMillis())
            .status(ExecutionStatus.SUCCESS)
            .stepType("SECTION CHAIN")
            .executableResponsesMetadata(
                Lists.newArrayList(new org.bson.Document().append("status", "Done").append("approvalField", "status"),
                    new org.bson.Document().append("terraformApplyResponse", "someResponse")))
            .taskIdToProgressDataMap(Maps.of(generateUuid(),
                Arrays.asList(DummyProgressData.builder().data("50% done").build(),
                    DummyProgressData.builder().data("100% done").build()),
                generateUuid(),
                Arrays.asList(DummyProgressData.builder().data("33% done").build(),
                    DummyProgressData.builder().data("100% done").build())))
            .build();
    executionNodeMap.put(provisioningInfraUuid, provisioningInfra);
    nodeAdjacencyListMap.put(provisioningInfraUuid,
        ExecutionNodeAdjacencyList.builder()
            .children(Lists.newArrayList(terraformPlanUuid))
            .nextIds(Lists.newArrayList(infraUuid))
            .build());

    ExecutionNode terraformPlan = ExecutionNode.builder()
                                      .uuid(terraformPlanUuid)
                                      .name("Terraform Plan")
                                      .startTs(System.currentTimeMillis())
                                      .endTs(System.currentTimeMillis())
                                      .status(ExecutionStatus.SUCCESS)
                                      .stepType("TASK")
                                      .build();
    executionNodeMap.put(terraformPlanUuid, terraformPlan);
    nodeAdjacencyListMap.put(
        terraformPlanUuid, ExecutionNodeAdjacencyList.builder().nextIds(Lists.newArrayList(approvalUuid)).build());

    ExecutionNode approval =
        ExecutionNode.builder()
            .uuid(approvalUuid)
            .name("Approve")
            .startTs(System.currentTimeMillis())
            .endTs(System.currentTimeMillis())
            .status(ExecutionStatus.SUCCESS)
            .stepType("TASK")
            .executableResponsesMetadata(
                Lists.newArrayList(new org.bson.Document().append("status", "Done").append("approvalField", "status")))
            .taskIdToProgressDataMap(Maps.of(generateUuid(),
                Arrays.asList(DummyProgressData.builder().data("50% done").build(),
                    DummyProgressData.builder().data("100% done").build())))
            .build();
    executionNodeMap.put(approvalUuid, approval);
    nodeAdjacencyListMap.put(
        approvalUuid, ExecutionNodeAdjacencyList.builder().nextIds(Lists.newArrayList(terraformApplyUuid)).build());

    ExecutionNode terraformApply = ExecutionNode.builder()
                                       .uuid(terraformApplyUuid)
                                       .name("Terraform Apply")
                                       .startTs(System.currentTimeMillis())
                                       .endTs(System.currentTimeMillis())
                                       .status(ExecutionStatus.SUCCESS)
                                       .stepType("TASK")
                                       .executableResponsesMetadata(Lists.newArrayList(
                                           new org.bson.Document().append("terraformApplyResponse", "someResponse")))
                                       .taskIdToProgressDataMap(Maps.of(generateUuid(),
                                           Arrays.asList(DummyProgressData.builder().data("33% done").build(),
                                               DummyProgressData.builder().data("100% done").build())))
                                       .build();
    executionNodeMap.put(terraformApplyUuid, terraformApply);
    nodeAdjacencyListMap.put(terraformApplyUuid, ExecutionNodeAdjacencyList.builder().build());

    ExecutionNode infra = ExecutionNode.builder()
                              .uuid(infraUuid)
                              .name("Infrastructure")
                              .startTs(System.currentTimeMillis())
                              .endTs(System.currentTimeMillis())
                              .status(ExecutionStatus.SUCCESS)
                              .stepType("INFRA")
                              .build();
    executionNodeMap.put(infraUuid, infra);
    nodeAdjacencyListMap.put(
        infraUuid, ExecutionNodeAdjacencyList.builder().nextIds(Lists.newArrayList(executionUuid)).build());

    ExecutionNode execution = ExecutionNode.builder()
                                  .uuid(executionUuid)
                                  .name("Execution")
                                  .startTs(System.currentTimeMillis())
                                  .endTs(System.currentTimeMillis())
                                  .status(ExecutionStatus.RUNNING)
                                  .stepType("SECTION")
                                  .build();
    executionNodeMap.put(executionUuid, execution);
    nodeAdjacencyListMap.put(
        executionUuid, ExecutionNodeAdjacencyList.builder().children(Lists.newArrayList(k8sUpdateUuid)).build());

    ExecutionNode k8sUpdate = ExecutionNode.builder()
                                  .uuid(k8sUpdateUuid)
                                  .name("k8sUpdate")
                                  .startTs(System.currentTimeMillis())
                                  .endTs(System.currentTimeMillis())
                                  .status(ExecutionStatus.SUCCESS)
                                  .stepType("TASK")
                                  .build();
    executionNodeMap.put(k8sUpdateUuid, k8sUpdate);
    nodeAdjacencyListMap.put(
        k8sUpdateUuid, ExecutionNodeAdjacencyList.builder().nextIds(Lists.newArrayList(forkUuid)).build());

    ExecutionNode fork = ExecutionNode.builder()
                             .uuid(forkUuid)
                             .name("Fork")
                             .startTs(System.currentTimeMillis())
                             .endTs(System.currentTimeMillis())
                             .status(ExecutionStatus.RUNNING)
                             .stepType("FORK")
                             .build();
    executionNodeMap.put(forkUuid, fork);
    nodeAdjacencyListMap.put(
        forkUuid, ExecutionNodeAdjacencyList.builder().children(Lists.newArrayList(step1Uuid, step2Uuid)).build());

    ExecutionNode step1 = ExecutionNode.builder()
                              .uuid(step1Uuid)
                              .name("Step1")
                              .startTs(System.currentTimeMillis())
                              .endTs(System.currentTimeMillis())
                              .status(ExecutionStatus.RUNNING)
                              .stepType("TERRAFORM")
                              .build();
    executionNodeMap.put(step1Uuid, step1);
    nodeAdjacencyListMap.put(step1Uuid, ExecutionNodeAdjacencyList.builder().build());

    ExecutionNode step2 = ExecutionNode.builder()
                              .uuid(step2Uuid)
                              .name("Step2")
                              .startTs(System.currentTimeMillis())
                              .endTs(System.currentTimeMillis())
                              .status(ExecutionStatus.WAITING)
                              .stepType("APPROVAL")
                              .build();
    executionNodeMap.put(step2Uuid, step2);
    nodeAdjacencyListMap.put(step2Uuid, ExecutionNodeAdjacencyList.builder().build());

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
