package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.exception.ExceptionUtils.getMessage;
import static java.util.Collections.singletonList;
import static software.wings.service.impl.aws.model.AwsConstants.ECS_SERVICE_SETUP_SWEEPING_OUTPUT_NAME;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ImageDetails;
import software.wings.helpers.ext.ecs.request.EcsServiceSetupRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceSetupResponse;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class EcsServiceSetup extends State {
  public static final String ECS_SERVICE_SETUP_COMMAND = "ECS Service Setup";

  @Getter @Setter private String roleArn;
  @Getter @Setter private String targetPort;
  @Getter @Setter private String maxInstances;
  @Getter @Setter private String fixedInstances;
  @Getter @Setter private String ecsServiceName;
  @Getter @Setter private String targetGroupArn;
  @Getter @Setter private boolean useLoadBalancer;
  @Getter @Setter private String loadBalancerName;
  @Getter @Setter private String targetContainerName;
  @Getter @Setter private String desiredInstanceCount;
  @Getter @Setter private int serviceSteadyStateTimeout;
  @Getter @Setter private ResizeStrategy resizeStrategy;
  @Getter @Setter private List<AwsAutoScalarConfig> awsAutoScalarConfigs;

  @Inject private SecretManager secretManager;
  @Inject private EcsStateHelper ecsStateHelper;
  @Inject private ActivityService activityService;
  @Inject private SettingsService settingsService;
  @Inject private DelegateService delegateService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private SweepingOutputService sweepingOutputService;

  public EcsServiceSetup(String name) {
    super(name, ECS_SERVICE_SETUP.name());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    EcsSetUpDataBag dataBag = ecsStateHelper.prepareBagForEcsSetUp(context, serviceSteadyStateTimeout,
        artifactCollectionUtils, serviceResourceService, infrastructureMappingService, settingsService, secretManager);

    Activity activity = ecsStateHelper.createActivity(
        context, ECS_SERVICE_SETUP_COMMAND, getStateType(), CommandUnitType.AWS_ECS_SERVICE_SETUP, activityService);

    EcsSetupParams ecsSetupParams = (EcsSetupParams) ecsStateHelper.buildContainerSetupParams(context,
        EcsSetupStateConfig.builder()
            .service(dataBag.getService())
            .app(dataBag.getApplication())
            .env(dataBag.getEnvironment())
            .infrastructureMapping(dataBag.getEcsInfrastructureMapping())
            .clusterName(dataBag.getEcsInfrastructureMapping().getClusterName())
            .containerTask(dataBag.getContainerTask())
            .roleArn(roleArn)
            .ecsServiceName(ecsServiceName)
            .imageDetails(dataBag.getImageDetails())
            .loadBalancerName(loadBalancerName)
            .serviceSteadyStateTimeout(dataBag.getServiceSteadyStateTimeout())
            .targetContainerName(targetContainerName)
            .serviceName(dataBag.getService().getName())
            .targetGroupArn(targetGroupArn)
            .targetPort(targetPort)
            .useLoadBalancer(useLoadBalancer)
            .ecsServiceSpecification(dataBag.getServiceSpecification())
            .isDaemonSchedulingStrategy(false)
            .awsAutoScalarConfigs(awsAutoScalarConfigs)
            .build());

    CommandStateExecutionData stateExecutionData =
        ecsStateHelper.getStateExecutionData(dataBag, ECS_SERVICE_SETUP_COMMAND, ecsSetupParams, activity);

    EcsSetupContextVariableHolder variables = ecsStateHelper.renderEcsSetupContextVariables(context);

    EcsServiceSetupRequest request = EcsServiceSetupRequest.builder()
                                         .ecsSetupParams(ecsSetupParams)
                                         .accountId(dataBag.getApplication().getAccountId())
                                         .appId(dataBag.getApplication().getUuid())
                                         .commandName(ECS_SERVICE_SETUP_COMMAND)
                                         .activityId(activity.getUuid())
                                         .awsConfig(dataBag.getAwsConfig())
                                         .clusterName(ecsSetupParams.getClusterName())
                                         .region(ecsSetupParams.getRegion())
                                         .safeDisplayServiceVariables(variables.getSafeDisplayServiceVariables())
                                         .serviceVariables(variables.getServiceVariables())
                                         .build();

    String delegateTaskId =
        ecsStateHelper.createAndQueueDelegateTaskForEcsServiceSetUp(request, dataBag, activity, delegateService);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(singletonList(activity.getUuid()))
        .stateExecutionData(stateExecutionData)
        .delegateTaskId(delegateTaskId)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    EcsCommandExecutionResponse executionResponse = (EcsCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        CommandExecutionStatus.SUCCESS == executionResponse.getCommandExecutionStatus() ? SUCCESS : FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    EcsServiceSetupResponse ecsServiceSetupResponse =
        (EcsServiceSetupResponse) executionResponse.getEcsCommandResponse();
    ContainerSetupCommandUnitExecutionData setupExecutionData = ecsServiceSetupResponse.getSetupData();
    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceId);
    if (artifact == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Artifact is null");
    }

    ImageDetails imageDetails =
        artifactCollectionUtils.fetchContainerImageDetails(artifact, context.getWorkflowExecutionId());
    ContainerServiceElement containerServiceElement = ecsStateHelper.buildContainerServiceElement(context,
        setupExecutionData, executionStatus, imageDetails, getMaxInstances(), getFixedInstances(),
        getDesiredInstanceCount(), getResizeStrategy(), getServiceSteadyStateTimeout(), logger);
    ecsStateHelper.populateFromDelegateResponse(setupExecutionData, executionData, containerServiceElement);
    sweepingOutputService.save(
        context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
            .name(ecsStateHelper.getSweepingOutputName(context, false, ECS_SERVICE_SETUP_SWEEPING_OUTPUT_NAME))
            .value(containerServiceElement)
            .build());

    executionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    return ExecutionResponse.builder().stateExecutionData(executionData).executionStatus(executionStatus).build();
  }
}