package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.exception.ExceptionUtils.getMessage;
import static java.util.Collections.singletonList;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ECS_BG_SERVICE_SETUP_ROUTE53;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import software.wings.common.Constants;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53ServiceSetupRequest;
import software.wings.helpers.ext.ecs.response.EcsBGRoute53ServiceSetupResponse;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.service.impl.artifact.ArtifactCollectionUtil;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsBlueGreenServiceSetupRoute53DNS extends State {
  private static final Logger logger = LoggerFactory.getLogger(EcsBlueGreenServiceSetupRoute53DNS.class);
  public static final String ECS_SERVICE_SETUP_COMMAND_ROUTE53 = "ECS Service Setup Route 53";

  @Getter @Setter private String roleArn;
  @Getter @Setter private String targetPort;
  @Getter @Setter private String maxInstances;
  @Getter @Setter private String fixedInstances;
  @Getter @Setter private String ecsServiceName;
  @Getter @Setter private String targetContainerName;
  @Getter @Setter private String desiredInstanceCount;
  @Getter @Setter private int serviceSteadyStateTimeout;
  @Getter @Setter private ResizeStrategy resizeStrategy;
  @Getter @Setter private List<AwsAutoScalarConfig> awsAutoScalarConfigs;

  @Getter @Setter private String parentRecordName;
  @Getter @Setter private String parentRecordHostedZoneId;
  @Getter @Setter private String serviceDiscoveryService1JSON;
  @Getter @Setter private String serviceDiscoveryService2JSON;

  @Inject private transient SecretManager secretManager;
  @Inject private transient EcsStateHelper ecsStateHelper;
  @Inject private transient ActivityService activityService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient ArtifactCollectionUtil artifactCollectionUtil;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;

  public EcsBlueGreenServiceSetupRoute53DNS(String name) {
    super(name, ECS_BG_SERVICE_SETUP_ROUTE53.name());
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    EcsSetUpDataBag dataBag = ecsStateHelper.prepareBagForEcsSetUp(context, serviceSteadyStateTimeout,
        artifactCollectionUtil, serviceResourceService, infrastructureMappingService, settingsService, secretManager);

    Activity activity = ecsStateHelper.createActivity(context, ECS_SERVICE_SETUP_COMMAND_ROUTE53, getStateType(),
        CommandUnitType.AWS_ECS_SERVICE_SETUP_ROUTE53, activityService);

    EcsSetupParams ecsSetupParams = (EcsSetupParams) ecsStateHelper.buildContainerSetupParams(context,
        EcsSetupStateConfig.builder()
            .useRoute53DNSSwap(true)
            .serviceDiscoveryService1JSON(serviceDiscoveryService1JSON)
            .serviceDiscoveryService2JSON(serviceDiscoveryService2JSON)
            .parentRecordHostedZoneId(parentRecordHostedZoneId)
            .parentRecordName(parentRecordName)
            .blueGreen(true)
            .app(dataBag.getApplication())
            .env(dataBag.getEnvironment())
            .service(dataBag.getService())
            .infrastructureMapping(dataBag.getEcsInfrastructureMapping())
            .clusterName(dataBag.getEcsInfrastructureMapping().getClusterName())
            .containerTask(dataBag.getContainerTask())
            .ecsServiceName(ecsServiceName)
            .imageDetails(dataBag.getImageDetails())
            .roleArn(roleArn)
            .serviceSteadyStateTimeout(dataBag.getServiceSteadyStateTimeout())
            .targetContainerName(targetContainerName)
            .targetPort(targetPort)
            .useLoadBalancer(false)
            .serviceName(dataBag.getService().getName())
            .ecsServiceSpecification(dataBag.getServiceSpecification())
            .isDaemonSchedulingStrategy(false)
            .awsAutoScalarConfigs(awsAutoScalarConfigs)
            .build());

    CommandStateExecutionData stateExecutionData =
        ecsStateHelper.getStateExecutionData(dataBag, ECS_SERVICE_SETUP_COMMAND_ROUTE53, ecsSetupParams, activity);

    EcsSetupContextVariableHolder variables = ecsStateHelper.renderEcsSetupContextVariables(context);

    EcsBGRoute53ServiceSetupRequest request =
        EcsBGRoute53ServiceSetupRequest.builder()
            .ecsSetupParams(ecsSetupParams)
            .awsConfig(dataBag.getAwsConfig())
            .clusterName(ecsSetupParams.getClusterName())
            .region(ecsSetupParams.getRegion())
            .accountId(dataBag.getApplication().getAccountId())
            .appId(dataBag.getApplication().getUuid())
            .commandName(ECS_SERVICE_SETUP_COMMAND_ROUTE53)
            .activityId(activity.getUuid())
            .safeDisplayServiceVariables(variables.getSafeDisplayServiceVariables())
            .serviceVariables(variables.getServiceVariables())
            .build();

    String delegateTaskId =
        ecsStateHelper.createAndQueueDelegateTaskForEcsServiceSetUp(request, dataBag, activity, delegateService);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(activity.getUuid()))
        .withStateExecutionData(stateExecutionData)
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

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
        CommandExecutionStatus.SUCCESS.equals(executionResponse.getCommandExecutionStatus()) ? SUCCESS : FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    EcsBGRoute53ServiceSetupResponse ecsServiceSetupResponse =
        (EcsBGRoute53ServiceSetupResponse) executionResponse.getEcsCommandResponse();
    ContainerSetupCommandUnitExecutionData setupExecutionData = ecsServiceSetupResponse.getSetupData();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
    if (artifact == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Artifact is null");
    }

    ImageDetails imageDetails = artifactCollectionUtil.fetchContainerImageDetails(
        artifact, context.getAppId(), context.getWorkflowExecutionId());
    ContainerServiceElement containerServiceElement = ecsStateHelper.buildContainerServiceElement(context,
        setupExecutionData, executionStatus, imageDetails, getMaxInstances(), getFixedInstances(),
        getDesiredInstanceCount(), getResizeStrategy(), getServiceSteadyStateTimeout(), logger);

    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
    ecsStateHelper.populateFromDelegateResponse(setupExecutionData, executionData, containerServiceElement);

    return anExecutionResponse()
        .withStateExecutionData(context.getStateExecutionData())
        .withExecutionStatus(executionStatus)
        .addContextElement(containerServiceElement)
        .addNotifyElement(containerServiceElement)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}