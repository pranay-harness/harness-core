package io.harness.pms.plan.execution;

import static io.harness.pms.contracts.plan.TriggerType.MANUAL;

import io.harness.NGCommonEntityConstants;
import io.harness.execution.PlanExecution;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.helpers.TriggeredByHelper;
import io.harness.pms.ngpipeline.inputset.beans.resource.MergeInputSetRequestDTOPMS;
import io.harness.pms.plan.execution.beans.dto.InterruptDTO;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.preflight.PreFlightCause;
import io.harness.pms.preflight.PreFlightDTO;
import io.harness.pms.preflight.PreFlightEntityErrorInfo;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.connector.ConnectorWrapperResponse;
import io.harness.pms.preflight.inputset.PipelineInputResponse;
import io.harness.pms.preflight.inputset.PipelineWrapperResponse;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.util.Collections;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

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
  @Inject private final TriggeredByHelper triggeredByHelper;

  @POST
  @Path("/{identifier}")
  @ApiOperation(
      value = "Execute a pipeline with inputSet pipeline yaml", nickname = "postPipelineExecuteWithInputSetYaml")
  public ResponseDTO<PlanExecutionResponseDto>
  runPipelineWithInputSetPipelineYaml(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @ApiParam(hidden = true) String inputSetPipelineYaml) throws IOException {
    PlanExecution planExecution = pipelineExecuteHelper.runPipelineWithInputSetPipelineYaml(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, inputSetPipelineYaml,
        ExecutionTriggerInfo.newBuilder()
            .setTriggerType(MANUAL)
            .setTriggeredBy(triggeredByHelper.getFromSecurityContext())
            .build());
    PlanExecutionResponseDto planExecutionResponseDto =
        PlanExecutionResponseDto.builder().planExecution(planExecution).build();
    return ResponseDTO.newResponse(planExecutionResponseDto);
  }

  @POST
  @Path("/{identifier}/inputSetList")
  @ApiOperation(
      value = "Execute a pipeline with input set references list", nickname = "postPipelineExecuteWithInputSetList")
  public ResponseDTO<PlanExecutionResponseDto>
  runPipelineWithInputSetIdentifierList(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTOPMS mergeInputSetRequestDTO) throws IOException {
    ExecutionTriggerInfo triggerInfo = ExecutionTriggerInfo.newBuilder()
                                           .setTriggerType(MANUAL)
                                           .setTriggeredBy(triggeredByHelper.getFromSecurityContext())
                                           .build();
    PlanExecution planExecution = pipelineExecuteHelper.runPipelineWithInputSetReferencesList(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, mergeInputSetRequestDTO.getInputSetReferences(), triggerInfo);
    PlanExecutionResponseDto planExecutionResponseDto =
        PlanExecutionResponseDto.builder().planExecution(planExecution).build();
    return ResponseDTO.newResponse(planExecutionResponseDto);
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

  @POST
  @ApiOperation(value = "initiate pre flight check", nickname = "startPreflightCheck")
  @Path("/preflightCheck")
  public ResponseDTO<String> startPreFlightCheck(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @ApiParam(hidden = true) String inputSetPipelineYaml) {
    return ResponseDTO.newResponse("dummyPreFlightCheckId");
  }

  @GET
  @ApiOperation(value = "initiate pre flight check", nickname = "getPreflightCheckResponse")
  @Path("/getPreflightCheckResponse")
  public ResponseDTO<PreFlightDTO> getPreflightCheckResponse(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("preflightCheckId") String preflightCheckId,
      @ApiParam(hidden = true) String inputSetPipelineYaml) {
    return ResponseDTO.newResponse(
        PreFlightDTO.builder()
            .pipelineInputWrapperResponse(
                PipelineWrapperResponse.builder()
                    .status(PreFlightStatus.FAILURE)
                    .pipelineInputResponse(Collections.singletonList(
                        PipelineInputResponse.builder()
                            .fqn("pipeline.stages.s1.execution.steps.google.url")
                            .stageName("s1")
                            .stepName("google")
                            .success(false)
                            .errorInfo(
                                PreFlightEntityErrorInfo.builder()
                                    .causes(Collections.singletonList(
                                        PreFlightCause.builder().cause("No value provided for runtime input").build()))
                                    .build())
                            .build()))
                    .build())
            .connectorWrapperResponse(
                ConnectorWrapperResponse.builder()
                    .status(PreFlightStatus.FAILURE)
                    .checkResponses(Collections.singletonList(
                        ConnectorCheckResponse.builder()
                            .connectorIdentifier("conn1")
                            .fqn("pipeline.stages.s1.infrastructure.connectorRef")
                            .stageName("s1")
                            .errorInfo(PreFlightEntityErrorInfo.builder()
                                           .causes(Collections.singletonList(
                                               PreFlightCause.builder().cause("Connector not reachable").build()))
                                           .build())
                            .build()))
                    .build())
            .status(PreFlightStatus.SUCCESS)
            .build());
  }
}
