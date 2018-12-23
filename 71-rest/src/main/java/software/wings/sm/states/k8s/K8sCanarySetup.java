package software.wings.sm.states.k8s;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static java.lang.Integer.parseInt;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.K8S_CANARY_SETUP;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sCanarySetupTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sCanarySetupResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sCanarySetup extends State implements K8sStateExecutor {
  private static final transient Logger logger = LoggerFactory.getLogger(K8sCanarySetup.class);

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

  public static final String K8S_CANARY_SETUP_COMMAND_NAME = "Canary Setup";

  public K8sCanarySetup(String name) {
    super(name, K8S_CANARY_SETUP.name());
  }

  @Getter @Setter @Attributes(title = "Instance Count") private String defaultInstanceCount;
  @Getter @Setter @Attributes(title = "Prefer Existing Instance Count") private boolean preferExistingInstanceCount;

  @Override
  public String commandName() {
    return K8S_CANARY_SETUP_COMMAND_NAME;
  }

  @Override
  public String stateType() {
    return getStateType();
  }

  @Override
  public void validateParameters(ExecutionContext context) {
    parseInt(context.renderExpression(this.defaultInstanceCount));
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return k8sStateHelper.executeWrapperWithManifest(this, context);
  }

  public ExecutionResponse executeK8sTask(
      ExecutionContext context, String activityId, Map<K8sValuesLocation, String> valuesFiles) {
    ApplicationManifest applicationManifest = k8sStateHelper.getApplicationManifest(context);
    ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

    K8sTaskParameters k8sTaskParameters =
        K8sCanarySetupTaskParameters.builder()
            .activityId(activityId)
            .releaseName(convertBase64UuidToCanonicalForm(infraMapping.getUuid()))
            .commandName(K8S_CANARY_SETUP_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.CANARY_SETUP)
            .timeoutIntervalInMin(10)
            .k8sDelegateManifestConfig(k8sStateHelper.createDelegateManifestConfig(applicationManifest))
            .valuesYamlList(k8sStateHelper.getRenderedValuesFiles(applicationManifest, context, valuesFiles))
            .build();

    return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return k8sStateHelper.handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;

    K8sCanarySetupResponse k8sCanarySetupResponse = (K8sCanarySetupResponse) executionResponse.getK8sTaskResponse();

    Integer targetInstances = parseInt(context.renderExpression(this.defaultInstanceCount));
    if (preferExistingInstanceCount && k8sCanarySetupResponse.getCurrentInstances() != null) {
      targetInstances = k8sCanarySetupResponse.getCurrentInstances();
    }

    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();

    stateExecutionData.setReleaseNumber(k8sCanarySetupResponse.getReleaseNumber());
    stateExecutionData.setTargetInstances(targetInstances);
    stateExecutionData.setStatus(executionStatus);

    String activityId = stateExecutionData.getActivityId();
    activityService.updateStatus(activityId, appId, executionStatus);

    k8sStateHelper.saveK8sElement(context,
        K8sElement.builder()
            .releaseNumber(k8sCanarySetupResponse.getReleaseNumber())
            .targetInstances(targetInstances)
            .currentReleaseWorkload(k8sCanarySetupResponse.getCurrentReleaseWorkload())
            .previousReleaseWorkload(k8sCanarySetupResponse.getPreviousReleaseWorkload())
            .build());

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(context.getStateExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public List<CommandUnit> commandUnitList(StoreType storeType) {
    List<CommandUnit> canaryCommandUnits = new ArrayList<>();

    if (StoreType.Remote.equals(storeType)) {
      canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.FetchFiles));
    }

    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Prepare));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Apply));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WrapUp));

    return canaryCommandUnits;
  }
}
