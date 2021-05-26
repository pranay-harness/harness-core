package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.EntityType;
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
import io.harness.encryption.Scope;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.bean.NotificationRules;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.execution.ExecutionStatus;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
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
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
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

  @POST
  @ApiOperation(value = "Create a Pipeline", nickname = "createPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  public ResponseDTO<String> createPipeline(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo, @NotNull @ApiParam(hidden = true) String yaml)
      throws IOException {
    log.info("Creating pipeline");

    pmsYamlSchemaService.validateYamlSchema(accountId, orgId, projectId, yaml);

    // validate unique fqn in yaml
    pmsYamlSchemaService.validateUniqueFqn(yaml);

    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
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
    log.info("Get pipeline");

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
      @BeanParam GitEntityUpdateInfoDTO gitEntityInfo, @NotNull @ApiParam(hidden = true) String yaml)
      throws IOException {
    log.info("Updating pipeline");

    pmsYamlSchemaService.validateYamlSchema(accountId, orgId, projectId, yaml);

    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    if (!pipelineEntity.getIdentifier().equals(pipelineId)) {
      throw new InvalidRequestException("Pipeline identifier in URL does not match pipeline identifier in yaml");
    }

    PipelineEntity withVersion = pipelineEntity.withVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

    // validate unique fqn in yaml
    pmsYamlSchemaService.validateUniqueFqn(yaml);

    PipelineEntity updatedEntity = pmsPipelineService.updatePipelineYaml(withVersion);

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
    log.info("Deleting pipeline");

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
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo, FilterPropertiesDTO filterProperties) {
    log.info("Get List of pipelines");
    Criteria criteria = pmsPipelineService.formCriteria(accountId, orgId, projectId, filterIdentifier,
        (PipelineFilterPropertiesDto) filterProperties, false, module, searchTerm);

    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.lastUpdatedAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    Page<PMSPipelineSummaryResponseDTO> pipelines =
        pmsPipelineService.list(criteria, pageRequest, accountId, orgId, projectId)
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
    log.info("Get pipeline Summary");

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
    log.info("Get Steps for given module");

    return ResponseDTO.newResponse(pmsPipelineService.getSteps(module, category, accountId));
  }

  @POST
  @Path("/execution/summary")
  @ApiOperation(value = "Gets Executions list", nickname = "getListOfExecutions")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<Page<PipelineExecutionSummaryDTO>> getListOfExecutions(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo, @QueryParam("searchTerm") String searchTerm,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("sort") List<String> sort, @QueryParam("filterIdentifier") String filterIdentifier,
      @QueryParam("module") String moduleName, FilterPropertiesDTO filterProperties,
      @QueryParam("status") ExecutionStatus status, @QueryParam("myDeployments") boolean myDeployments) {
    log.info("Get List of executions");
    Criteria criteria = pmsExecutionService.formCriteria(accountId, orgId, projectId, pipelineIdentifier,
        filterIdentifier, (PipelineExecutionFilterPropertiesDTO) filterProperties, moduleName, searchTerm, status,
        myDeployments, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    Page<PipelineExecutionSummaryDTO> planExecutionSummaryDTOS =
        pmsExecutionService.getPipelineExecutionSummaryEntity(criteria, pageRequest).map(e -> {
          Optional<PipelineEntity> optionalPipelineEntity =
              pmsPipelineService.get(accountId, orgId, projectId, e.getPipelineIdentifier(), false);
          if (!optionalPipelineEntity.isPresent()) {
            throw new InvalidRequestException("Pipeline with identifier " + e.getPipelineIdentifier() + " not found");
          }
          return PipelineExecutionSummaryDtoMapper.toDto(e, optionalPipelineEntity.get());
        });

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
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info("Get Execution Detail");

    PipelineExecutionSummaryEntity executionSummaryEntity =
        pmsExecutionService.getPipelineExecutionSummaryEntity(accountId, orgId, projectId, planExecutionId, false);

    Optional<PipelineEntity> optionalPipelineEntity =
        pmsPipelineService.get(accountId, orgId, projectId, executionSummaryEntity.getPipelineIdentifier(), false);
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(
          "Pipeline with identifier " + executionSummaryEntity.getPipelineIdentifier() + " not found");
    }

    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of("PIPELINE", executionSummaryEntity.getPipelineIdentifier()), PipelineRbacPermissions.PIPELINE_VIEW);

    PipelineExecutionDetailDTO pipelineExecutionDetailDTO =
        PipelineExecutionDetailDTO.builder()
            .pipelineExecutionSummary(
                PipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntity, optionalPipelineEntity.get()))
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
      @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId) {
    return pmsExecutionService.getInputSetYaml(accountId, orgId, projectId, planExecutionId, false);
  }

  @GET
  @Path("/yaml-schema")
  @ApiOperation(value = "Get Yaml Schema", nickname = "getYamlSchema")
  public ResponseDTO<JsonNode> getYamlSchema(@QueryParam("entityType") @NotNull EntityType entityType,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope) {
    if (entityType != EntityType.PIPELINES) {
      throw new NotSupportedException(String.format("Entity type %s is not supported", entityType.getYamlName()));
    }
    JsonNode schema = pmsYamlSchemaService.getPipelineYamlSchema(projectIdentifier, orgIdentifier, scope);
    if (schema == null) {
      throw new NotFoundException(String.format("No schema found for entity type %s ", entityType.getYamlName()));
    }
    return ResponseDTO.newResponse(schema);
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
