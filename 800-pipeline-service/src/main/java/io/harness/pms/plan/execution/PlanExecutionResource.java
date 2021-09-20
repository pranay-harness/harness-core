package io.harness.pms.plan.execution;

import static java.lang.String.format;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.ngpipeline.inputset.beans.resource.MergeInputSetRequestDTOPMS;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.InterruptDTO;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.preflight.PreFlightDTO;
import io.harness.pms.preflight.service.PreflightService;
import io.harness.pms.rbac.PipelineRbacPermissions;
import io.harness.pms.stages.StageExecutionResponse;
import io.harness.pms.stages.StageExecutionSelectorHelper;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.orchestrationEventLog.OrchestrationEventLogRepository;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PIPELINE)
@Api("/pipeline/execute")
@Path("/pipeline/execute")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@PipelineServiceAuth
@Slf4j
public class PlanExecutionResource {
  @Inject private final PipelineExecuteHelper pipelineExecuteHelper;
  @Inject private final PMSExecutionService pmsExecutionService;
  @Inject private final OrchestrationEventLogRepository orchestrationEventLogRepository;
  @Inject private final AccessControlClient accessControlClient;
  @Inject private final PreflightService preflightService;
  @Inject private final PMSPipelineService pmsPipelineService;

  @POST
  @Path("/{identifier}")
  @ApiOperation(
      value = "Execute a pipeline with inputSet pipeline yaml", nickname = "postPipelineExecuteWithInputSetYaml")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<PlanExecutionResponseDto>
  runPipelineWithInputSetPipelineYaml(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.MODULE_TYPE) String moduleType,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @ApiParam(hidden = true) String inputSetPipelineYaml) {
    try {
      PlanExecutionResponseDto planExecutionResponseDto = pipelineExecuteHelper.runPipelineWithInputSetPipelineYaml(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, moduleType, inputSetPipelineYaml, false);
      return ResponseDTO.newResponse(planExecutionResponseDto);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @POST
  @Path("/{identifier}/v2")
  @ApiOperation(
      value = "Execute a pipeline with inputSet pipeline yaml V2", nickname = "postPipelineExecuteWithInputSetYamlv2")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<PlanExecutionResponseDto>
  runPipelineWithInputSetPipelineYamlV2(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.MODULE_TYPE) String moduleType,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @ApiParam(hidden = true) String inputSetPipelineYaml) {
    try {
      PlanExecutionResponseDto planExecutionResponseDto = pipelineExecuteHelper.runPipelineWithInputSetPipelineYaml(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, moduleType, inputSetPipelineYaml, true);
      return ResponseDTO.newResponse(planExecutionResponseDto);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @POST
  @Path("/rerun/{originalExecutionId}/{identifier}")
  @ApiOperation(
      value = "Re Execute a pipeline with inputSet pipeline yaml", nickname = "rePostPipelineExecuteWithInputSetYaml")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<PlanExecutionResponseDto>
  rerunPipelineWithInputSetPipelineYaml(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.MODULE_TYPE) String moduleType,
      @NotNull @PathParam("originalExecutionId") String originalExecutionId,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @ApiParam(hidden = true) String inputSetPipelineYaml) {
    try {
      PlanExecutionResponseDto planExecutionResponseDto =
          pipelineExecuteHelper.rerunPipelineWithInputSetPipelineYaml(accountId, orgIdentifier, projectIdentifier,
              pipelineIdentifier, moduleType, originalExecutionId, inputSetPipelineYaml, false);
      return ResponseDTO.newResponse(planExecutionResponseDto);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @POST
  @Path("/rerun/v2/{originalExecutionId}/{identifier}")
  @ApiOperation(value = "Re Execute a pipeline with inputSet pipeline yaml Version 2",
      nickname = "rePostPipelineExecuteWithInputSetYamlV2")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<PlanExecutionResponseDto>
  rerunPipelineWithInputSetPipelineYamlV2(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.MODULE_TYPE) String moduleType,
      @NotNull @PathParam("originalExecutionId") String originalExecutionId,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @ApiParam(hidden = true) String inputSetPipelineYaml) {
    try {
      PlanExecutionResponseDto planExecutionResponseDto =
          pipelineExecuteHelper.rerunPipelineWithInputSetPipelineYaml(accountId, orgIdentifier, projectIdentifier,
              pipelineIdentifier, moduleType, originalExecutionId, inputSetPipelineYaml, true);
      return ResponseDTO.newResponse(planExecutionResponseDto);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @GET
  @Path("/{planExecutionId}/retryStages")
  @ApiOperation(value = "Get retry stages for failed pipeline", nickname = "getRetryStages")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<RetryInfo> getRetryStages(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @NotNull @PathParam(NGCommonEntityConstants.PLAN_KEY) String planExecutionId,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) throws IOException {
    Optional<PipelineEntity> updatedPipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);

    if (!updatedPipelineEntity.isPresent()) {
      return ResponseDTO.newResponse(
          RetryInfo.builder()
              .isResumable(false)
              .errorMessage(String.format(
                  "Pipeline with the given ID: %s does not exist or has been deleted", pipelineIdentifier))
              .build());
    }
    String updatedPipeline = updatedPipelineEntity.get().getYaml();

    String executedPipeline = pipelineExecuteHelper.getYamlFromExecutionId(planExecutionId);
    return ResponseDTO.newResponse(
        pipelineExecuteHelper.getRetryStages(updatedPipeline, executedPipeline, planExecutionId, pipelineIdentifier));
  }

  @POST
  @Path("/{identifier}/inputSetList")
  @ApiOperation(
      value = "Execute a pipeline with input set references list", nickname = "postPipelineExecuteWithInputSetList")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<PlanExecutionResponseDto>
  runPipelineWithInputSetIdentifierList(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.MODULE_TYPE) String moduleType,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTOPMS mergeInputSetRequestDTO) {
    try {
      PlanExecutionResponseDto planExecutionResponseDto =
          pipelineExecuteHelper.runPipelineWithInputSetReferencesList(accountId, orgIdentifier, projectIdentifier,
              pipelineIdentifier, moduleType, mergeInputSetRequestDTO.getInputSetReferences(),
              gitEntityBasicInfo.getBranch(), gitEntityBasicInfo.getYamlGitConfigId());
      return ResponseDTO.newResponse(planExecutionResponseDto);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @POST
  @Path("rerun/{originalExecutionId}/{identifier}/inputSetList")
  @ApiOperation(
      value = "Execute a pipeline with input set references list", nickname = "rePostPipelineExecuteWithInputSetList")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<PlanExecutionResponseDto>
  rerunPipelineWithInputSetIdentifierList(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.MODULE_TYPE) String moduleType,
      @NotNull @PathParam("originalExecutionId") String originalExecutionId,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTOPMS mergeInputSetRequestDTO) {
    try {
      PlanExecutionResponseDto planExecutionResponseDto =
          pipelineExecuteHelper.rerunPipelineWithInputSetReferencesList(accountId, orgIdentifier, projectIdentifier,
              pipelineIdentifier, moduleType, originalExecutionId, mergeInputSetRequestDTO.getInputSetReferences(),
              gitEntityBasicInfo.getBranch(), gitEntityBasicInfo.getYamlGitConfigId());
      return ResponseDTO.newResponse(planExecutionResponseDto);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @PUT
  @ApiOperation(value = "pause, resume or stop the pipeline executions", nickname = "handleInterrupt")
  @Path("/interrupt/{planExecutionId}")
  public ResponseDTO<InterruptDTO> handleInterrupt(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @QueryParam("interruptType") PlanExecutionInterruptType executionInterruptType,
      @NotNull @PathParam("planExecutionId") String planExecutionId) {
    PipelineExecutionSummaryEntity executionSummaryEntity =
        pmsExecutionService.getPipelineExecutionSummaryEntity(accountId, orgId, projectId, planExecutionId, false);

    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of("PIPELINE", executionSummaryEntity.getPipelineIdentifier()),
        PipelineRbacPermissions.PIPELINE_EXECUTE);

    return ResponseDTO.newResponse(
        pmsExecutionService.registerInterrupt(executionInterruptType, planExecutionId, null));
  }

  // TODO(prashant) : This is a temp route for now merge it with the above. Need be done in sync with UI changes
  @PUT
  @ApiOperation(value = "pause, resume or stop the stage executions", nickname = "handleStageInterrupt")
  @Path("/interrupt/{planExecutionId}/{nodeExecutionId}")
  public ResponseDTO<InterruptDTO> handleStageInterrupt(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @QueryParam("interruptType") PlanExecutionInterruptType executionInterruptType,
      @NotNull @PathParam("planExecutionId") String planExecutionId,
      @NotNull @PathParam("nodeExecutionId") String nodeExecutionId) {
    return ResponseDTO.newResponse(
        pmsExecutionService.registerInterrupt(executionInterruptType, planExecutionId, nodeExecutionId));
  }

  @PUT
  @ApiOperation(value = "Ignore,Abort,MarkAsSuccess,Retry on post manual intervention",
      nickname = "handleManualInterventionInterrupt")
  @Path("/manualIntervention/interrupt/{planExecutionId}/{nodeExecutionId}")
  public ResponseDTO<InterruptDTO>
  handleManualInterventionInterrupt(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @QueryParam("interruptType") PlanExecutionInterruptType executionInterruptType,
      @NotNull @PathParam("planExecutionId") String planExecutionId,
      @NotNull @PathParam("nodeExecutionId") String nodeExecutionId) {
    return ResponseDTO.newResponse(
        pmsExecutionService.registerInterrupt(executionInterruptType, planExecutionId, nodeExecutionId));
  }

  @POST
  @ApiOperation(value = "initiate pre flight check", nickname = "startPreflightCheck")
  @Path("/preflightCheck")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<String> startPreFlightCheck(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo, @ApiParam(hidden = true) String inputSetPipelineYaml) {
    try {
      return ResponseDTO.newResponse(preflightService.startPreflightCheck(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetPipelineYaml));
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @GET
  @ApiOperation(value = "get preflight check response", nickname = "getPreflightCheckResponse")
  @Path("/getPreflightCheckResponse")
  public ResponseDTO<PreFlightDTO> getPreflightCheckResponse(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("preflightCheckId") String preflightCheckId,
      @ApiParam(hidden = true) String inputSetPipelineYaml) {
    return ResponseDTO.newResponse(preflightService.getPreflightCheckResponse(preflightCheckId));
  }

  @GET
  @ApiOperation(value = "Run a schema on db.", nickname = "runSchemaOnDb")
  @Path("/internal/runSchema")
  public ResponseDTO<String> runASchemaMigration() {
    orchestrationEventLogRepository.schemaMigrationForOldEvenLog();
    return ResponseDTO.newResponse("Deleted Old Orchestration event log entries");
  }

  @GET
  @ApiOperation(value = "get list of stages for stage execution", nickname = "getStagesExecutionList")
  @Path("/stagesExecutionList")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<List<StageExecutionResponse>> getStagesExecutionList(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier @NotEmpty String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    Optional<PipelineEntity> optionalPipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(format("Pipeline [%s] under Project[%s], Organization [%s] doesn't exist.",
          pipelineIdentifier, projectIdentifier, orgIdentifier));
    }
    PipelineEntity pipelineEntity = optionalPipelineEntity.get();
    List<StageExecutionResponse> stageExecutionResponse =
        StageExecutionSelectorHelper.getStageExecutionResponse(pipelineEntity.getYaml());
    return ResponseDTO.newResponse(stageExecutionResponse);
  }
}
