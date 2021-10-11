package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionNode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.bean.NotificationRules;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetYamlWithTemplateDTO;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.ExecutionGraphMapper;
import io.harness.pms.pipeline.mappers.NodeExecutionToExecutioNodeMapper;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.pipeline.mappers.PipelineExecutionSummaryDtoMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionDetailDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionFilterPropertiesDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.rbac.PipelineRbacPermissions;
import io.harness.pms.variables.VariableMergeServiceResponse;
import io.harness.utils.PageUtils;
import io.harness.yaml.schema.YamlSchemaResource;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
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
@PipelineServiceAuth
@Slf4j
public class PipelineResource implements YamlSchemaResource {
  private final PMSPipelineService pmsPipelineService;
  private final PMSExecutionService pmsExecutionService;
  private final PMSYamlSchemaService pmsYamlSchemaService;
  private final NodeExecutionService nodeExecutionService;
  private final AccessControlClient accessControlClient;
  private final NodeExecutionToExecutioNodeMapper nodeExecutionToExecutioNodeMapper;
  private final PmsGitSyncHelper pmsGitSyncHelper;

  @POST
  @ApiOperation(value = "Create a Pipeline", nickname = "createPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  public ResponseDTO<String> createPipeline(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo, @NotNull String yaml) throws IOException {
    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    log.info(String.format("Creating pipeline with identifier %s in project %s, org %s, account %s",
        pipelineEntity.getIdentifier(), projectId, orgId, accountId));

    pmsYamlSchemaService.validateYamlSchema(accountId, orgId, projectId, yaml);

    // validate unique fqn in yaml
    pmsYamlSchemaService.validateUniqueFqn(yaml);
    PipelineEntity createdEntity = pmsPipelineService.create(pipelineEntity);

    return ResponseDTO.newResponse(createdEntity.getVersion().toString(), createdEntity.getIdentifier());
  }

  @POST
  @Path("/variables")
  @ApiOperation(value = "Create variables for Pipeline", nickname = "createVariables")
  public ResponseDTO<VariableMergeServiceResponse> createVariables(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @ApiParam(hidden = true) String yaml) {
    log.info("Creating variables for pipeline.");

    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    VariableMergeServiceResponse variablesResponse = pmsPipelineService.createVariablesResponse(pipelineEntity);

    return ResponseDTO.newResponse(variablesResponse);
  }

  @GET
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Gets a pipeline by identifier", nickname = "getPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<PMSPipelineResponseDTO> getPipelineByIdentifier(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineId,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info(String.format("Retrieving pipeline with identifier %s in project %s, org %s, account %s", pipelineId,
        projectId, orgId, accountId));

    Optional<PipelineEntity> pipelineEntity = pmsPipelineService.get(accountId, orgId, projectId, pipelineId, false);
    String version = "0";
    if (pipelineEntity.isPresent()) {
      version = pipelineEntity.get().getVersion().toString();
    }

    PMSPipelineResponseDTO pipeline = PMSPipelineDtoMapper.writePipelineDto(pipelineEntity.orElseThrow(
        ()
            -> new InvalidRequestException(
                String.format("Pipeline with the given ID: %s does not exist or has been deleted", pipelineId))));

    return ResponseDTO.newResponse(version, pipeline);
  }

  @PUT
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Update a Pipeline", nickname = "putPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  public ResponseDTO<String> updatePipeline(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineId,
      @BeanParam GitEntityUpdateInfoDTO gitEntityInfo, @NotNull String yaml) throws IOException {
    log.info(String.format("Updating pipeline with identifier %s in project %s, org %s, account %s", pipelineId,
        projectId, orgId, accountId));

    pmsYamlSchemaService.validateYamlSchema(accountId, orgId, projectId, yaml);

    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    if (!pipelineEntity.getIdentifier().equals(pipelineId)) {
      throw new InvalidRequestException("Pipeline identifier in URL does not match pipeline identifier in yaml");
    }

    PipelineEntity withVersion = pipelineEntity.withVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

    // validate unique fqn in yaml
    pmsYamlSchemaService.validateUniqueFqn(yaml);

    PipelineEntity updatedEntity = pmsPipelineService.updatePipelineYaml(withVersion, ChangeType.MODIFY);

    return ResponseDTO.newResponse(updatedEntity.getVersion().toString(), updatedEntity.getIdentifier());
  }

  @DELETE
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Delete a pipeline", nickname = "softDeletePipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_DELETE)
  public ResponseDTO<Boolean> deletePipeline(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineId,
      @BeanParam GitEntityDeleteInfoDTO entityDeleteInfo) {
    log.info(String.format("Deleting pipeline with identifier %s in project %s, org %s, account %s", pipelineId,
        projectId, orgId, accountId));

    return ResponseDTO.newResponse(pmsPipelineService.delete(
        accountId, orgId, projectId, pipelineId, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @POST
  @Path("/list")
  @ApiOperation(value = "Gets Pipeline list", nickname = "getPipelineList")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<Page<PMSPipelineSummaryResponseDTO>> getListOfPipelines(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size,
      @QueryParam("sort") List<String> sort, @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("module") String module, @QueryParam("filterIdentifier") String filterIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo, FilterPropertiesDTO filterProperties,
      @QueryParam("getDistinctFromBranches") Boolean getDistinctFromBranches) {
    log.info(String.format("Get List of pipelines in project %s, org %s, account %s", projectId, orgId, accountId));
    Criteria criteria = pmsPipelineService.formCriteria(accountId, orgId, projectId, filterIdentifier,
        (PipelineFilterPropertiesDto) filterProperties, false, module, searchTerm);

    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.lastUpdatedAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    Page<PMSPipelineSummaryResponseDTO> pipelines =
        pmsPipelineService.list(criteria, pageRequest, accountId, orgId, projectId, getDistinctFromBranches)
            .map(PMSPipelineDtoMapper::preparePipelineSummary);

    return ResponseDTO.newResponse(pipelines);
  }

  @GET
  @Path("/summary/{pipelineIdentifier}")
  @ApiOperation(value = "Gets Pipeline Summary of a pipeline", nickname = "getPipelineSummary")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<PMSPipelineSummaryResponseDTO> getPipelineSummary(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineId,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info(
        String.format("Get pipeline summary for pipeline with with identifier %s in project %s, org %s, account %s",
            pipelineId, projectId, orgId, accountId));

    PMSPipelineSummaryResponseDTO pipelineSummary = PMSPipelineDtoMapper.preparePipelineSummary(
        pmsPipelineService.get(accountId, orgId, projectId, pipelineId, false)
            .orElseThrow(()
                             -> new InvalidRequestException(String.format(
                                 "Pipeline with the given ID: %s does not exist or has been deleted", pipelineId))));

    return ResponseDTO.newResponse(pipelineSummary);
  }

  @GET
  @Path("/steps")
  @ApiOperation(value = "Get Steps for given module", nickname = "getSteps")
  public ResponseDTO<StepCategory> getSteps(@NotNull @QueryParam("category") String category,
      @NotNull @QueryParam("module") String module, @QueryParam("accountId") String accountId) {
    log.info("Get Steps for module " + module);

    return ResponseDTO.newResponse(pmsPipelineService.getSteps(module, category, accountId));
  }

  @POST
  @Path("/v2/steps")
  @ApiOperation(value = "Get Steps for given modules Version 2", nickname = "getStepsV2")
  public ResponseDTO<StepCategory> getStepsV2(
      @QueryParam("accountId") String accountId, StepPalleteFilterWrapper stepPalleteFilterWrapper) {
    return ResponseDTO.newResponse(pmsPipelineService.getStepsV2(accountId, stepPalleteFilterWrapper));
  }

  @POST
  @Path("/execution/summary")
  @ApiOperation(value = "Gets Executions list", nickname = "getListOfExecutions")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<Page<PipelineExecutionSummaryDTO>> getListOfExecutions(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @QueryParam("searchTerm") String searchTerm,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("sort") List<String> sort, @QueryParam("filterIdentifier") String filterIdentifier,
      @QueryParam("module") String moduleName, FilterPropertiesDTO filterProperties,
      @QueryParam("status") List<ExecutionStatus> statusesList, @QueryParam("myDeployments") boolean myDeployments,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info("Get List of executions");
    ByteString gitSyncBranchContext = pmsGitSyncHelper.getGitSyncBranchContextBytesThreadLocal();
    if (EmptyPredicate.isEmpty(gitEntityBasicInfo.getBranch())
        || EmptyPredicate.isEmpty(gitEntityBasicInfo.getYamlGitConfigId())) {
      gitSyncBranchContext = null;
    }
    Criteria criteria = pmsExecutionService.formCriteria(accountId, orgId, projectId, pipelineIdentifier,
        filterIdentifier, (PipelineExecutionFilterPropertiesDTO) filterProperties, moduleName, searchTerm, statusesList,
        myDeployments, false, gitSyncBranchContext);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    // NOTE: We are getting entity git details from git context and not pipeline entity as we'll have to make DB calls
    // to fetch them and each might have a different branch context so we cannot even batch them. The only data missing
    // because of this approach is objectId which UI doesn't use.
    Page<PipelineExecutionSummaryDTO> planExecutionSummaryDTOS =
        pmsExecutionService.getPipelineExecutionSummaryEntity(criteria, pageRequest)
            .map(e
                -> PipelineExecutionSummaryDtoMapper.toDto(e,
                    e.getEntityGitDetails() != null
                        ? e.getEntityGitDetails()
                        : pmsGitSyncHelper.getEntityGitDetailsFromBytes(e.getGitSyncBranchContext())));

    return ResponseDTO.newResponse(planExecutionSummaryDTOS);
  }

  @GET
  @Path("/execution/{planExecutionId}")
  @ApiOperation(value = "Gets Execution Detail", nickname = "getExecutionDetail")
  public ResponseDTO<PipelineExecutionDetailDTO> getExecutionDetail(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @QueryParam("filter") String filter, @QueryParam("stageNodeId") String stageNodeId,
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    PipelineExecutionSummaryEntity executionSummaryEntity =
        pmsExecutionService.getPipelineExecutionSummaryEntity(accountId, orgId, projectId, planExecutionId, false);

    Optional<PipelineEntity> optionalPipelineEntity;
    if (executionSummaryEntity.getEntityGitDetails() == null) {
      try (PmsGitSyncBranchContextGuard ignore = pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(
               executionSummaryEntity.getGitSyncBranchContext(), false)) {
        optionalPipelineEntity =
            pmsPipelineService.get(accountId, orgId, projectId, executionSummaryEntity.getPipelineIdentifier(), false);
      }
    } else {
      try (PmsGitSyncBranchContextGuard ignore = new PmsGitSyncBranchContextGuard(
               executionSummaryEntity.getEntityGitDetails().toGitSyncBranchContext(), false)) {
        optionalPipelineEntity =
            pmsPipelineService.get(accountId, orgId, projectId, executionSummaryEntity.getPipelineIdentifier(), false);
      }
    }
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(
          "Pipeline with identifier " + executionSummaryEntity.getPipelineIdentifier() + " not found");
    }

    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of("PIPELINE", executionSummaryEntity.getPipelineIdentifier()), PipelineRbacPermissions.PIPELINE_VIEW);

    PipelineExecutionDetailDTO pipelineExecutionDetailDTO =
        PipelineExecutionDetailDTO.builder()
            .pipelineExecutionSummary(PipelineExecutionSummaryDtoMapper.toDto(
                executionSummaryEntity, EntityGitDetailsMapper.mapEntityGitDetails(optionalPipelineEntity.get())))
            .executionGraph(ExecutionGraphMapper.toExecutionGraph(
                pmsExecutionService.getOrchestrationGraph(stageNodeId, planExecutionId)))
            .build();

    return ResponseDTO.newResponse(pipelineExecutionDetailDTO);
  }

  @GET
  @Produces({"application/yaml"})
  @Path("/execution/{planExecutionId}/inputset")
  @ApiOperation(value = "Gets  inputsetYaml", nickname = "getInputsetYaml")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public String getInputsetYaml(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @QueryParam("resolveExpressions") @DefaultValue("false") boolean resolveExpressions,
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    return pmsExecutionService
        .getInputSetYamlWithTemplate(accountId, orgId, projectId, planExecutionId, false, resolveExpressions)
        .getInputSetYaml();
  }

  @GET
  @Path("/execution/{planExecutionId}/inputsetV2")
  @ApiOperation(value = "Gets  inputsetYaml", nickname = "getInputsetYamlV2")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<InputSetYamlWithTemplateDTO> getInputsetYamlV2(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @QueryParam("resolveExpressions") @DefaultValue("false") boolean resolveExpressions,
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    return ResponseDTO.newResponse(pmsExecutionService.getInputSetYamlWithTemplate(
        accountId, orgId, projectId, planExecutionId, false, resolveExpressions));
  }

  @GET
  @Path("/notification")
  @ApiOperation(value = "Get Notification Schema", nickname = "getNotificationSchema")
  public ResponseDTO<NotificationRules> getNotificationSchema() {
    return ResponseDTO.newResponse(NotificationRules.builder().build());
  }

  @GET
  @Path("/getExecutionNode")
  @ApiOperation(value = "get execution node", nickname = "getExecutionNode")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<ExecutionNode> getExecutionNode(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @NotNull @QueryParam("nodeExecutionId") String nodeExecutionId) {
    if (nodeExecutionId == null) {
      return null;
    }
    return ResponseDTO.newResponse(
        nodeExecutionToExecutioNodeMapper.mapNodeExecutionToExecutionNode(nodeExecutionService.get(nodeExecutionId)));
  }
}
