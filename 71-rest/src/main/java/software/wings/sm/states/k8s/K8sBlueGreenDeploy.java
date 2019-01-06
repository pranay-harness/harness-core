package software.wings.sm.states.k8s;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.exception.WingsException.USER;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.K8S_CANARY_SETUP;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sBlueGreenDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sBlueGreenDeploy extends State implements K8sStateExecutor {
  private static final transient Logger logger = LoggerFactory.getLogger(K8sBlueGreenDeploy.class);

  @Inject private transient ActivityService activityService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject private transient K8sStateHelper k8sStateHelper;
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient AwsCommandHelper awsCommandHelper;

  public static final String K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME = "Blue/Green Deployment";

  public K8sBlueGreenDeploy(String name) {
    super(name, K8S_CANARY_SETUP.name());
  }

  @Getter @Setter @Attributes(title = "Primary Service Name") private String primaryServiceName;
  @Getter @Setter @Attributes(title = "Stage Service Name") private String stageServiceName;

  @Override
  public String commandName() {
    return K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME;
  }

  @Override
  public String stateType() {
    return getStateType();
  }

  @Override
  public void validateParameters(ExecutionContext context) {
    if (StringUtils.isEmpty(this.primaryServiceName)) {
      throw new InvalidRequestException("Primary Service not specified.", USER);
    }

    if (StringUtils.isEmpty(this.stageServiceName)) {
      throw new InvalidRequestException("Stage Service not specified.", USER);
    }
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return k8sStateHelper.executeWrapperWithManifest(this, context);
  }

  public ExecutionResponse executeK8sTask(
      ExecutionContext context, String activityId, Map<K8sValuesLocation, String> valuesFiles) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = k8sStateHelper.getApplicationManifests(context);
    ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

    String renderedPrimaryServiceName = context.renderExpression(this.primaryServiceName);
    String renderedStageServiceName = context.renderExpression(this.stageServiceName);

    K8sTaskParameters k8sTaskParameters =
        K8sBlueGreenDeployTaskParameters.builder()
            .activityId(activityId)
            .releaseName(convertBase64UuidToCanonicalForm(infraMapping.getUuid()))
            .commandName(K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.BLUE_GREEN_DEPLOY)
            .timeoutIntervalInMin(10)
            .k8sDelegateManifestConfig(
                k8sStateHelper.createDelegateManifestConfig(appManifestMap.get(K8sValuesLocation.Service)))
            .valuesYamlList(k8sStateHelper.getRenderedValuesFiles(appManifestMap, context, valuesFiles))
            .primaryServiceName(renderedPrimaryServiceName)
            .stageServiceName(renderedStageServiceName)
            .build();

    return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return k8sStateHelper.handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;

    K8sBlueGreenDeployResponse k8sBlueGreenDeployResponse =
        (K8sBlueGreenDeployResponse) executionResponse.getK8sTaskResponse();

    activityService.updateStatus(
        k8sStateHelper.getActivityId(context), k8sStateHelper.getAppId(context), executionStatus);

    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setReleaseNumber(k8sBlueGreenDeployResponse.getReleaseNumber());
    stateExecutionData.setStatus(executionStatus);

    k8sStateHelper.saveK8sElement(context,
        K8sElement.builder()
            .releaseNumber(k8sBlueGreenDeployResponse.getReleaseNumber())
            .primaryServiceName(context.renderExpression(this.primaryServiceName))
            .stageServiceName(context.renderExpression(this.stageServiceName))
            .build());

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(stateExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public List<CommandUnit> commandUnitList(boolean remoteStoreType) {
    List<CommandUnit> blueGreenCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.FetchFiles));
    }

    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init));
    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Prepare));
    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Apply));
    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WaitForSteadyState));
    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WrapUp));

    return blueGreenCommandUnits;
  }
}
