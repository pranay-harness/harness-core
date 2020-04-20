package software.wings.sm.states.k8s;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.sm.StateType.K8S_APPLY;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.stencils.DefaultValue;
import software.wings.utils.ApplicationManifestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class K8sApplyState extends State implements K8sStateExecutor {
  @Inject private K8sStateHelper k8sStateHelper;
  @Inject private AppService appService;
  @Inject private ActivityService activityService;
  @Inject private ApplicationManifestUtils applicationManifestUtils;

  public static final String K8S_APPLY_STATE = "Apply";
  public static final String SKIP_FILE_FOR_DEPLOY_PLACEHOLDER_TEXT = "harness.io/skip-file-for-deploy";

  public K8sApplyState(String name) {
    super(name, K8S_APPLY.name());
  }

  @Trimmed @Getter @Setter @Attributes(title = "File paths", required = true) private String filePaths;
  @Getter
  @Setter
  @Attributes(title = "Timeout (Minutes)", required = true)
  @DefaultValue("10")
  private String stateTimeoutInMinutes;
  @Getter @Setter @Attributes(title = "Skip steady state check") private boolean skipSteadyStateCheck;
  @Getter @Setter @Attributes(title = "Skip Dry Run") private boolean skipDryRun;

  @Override
  public Integer getTimeoutMillis() {
    try {
      Integer timeoutMinutes = Integer.valueOf(stateTimeoutInMinutes);
      return getTimeoutMillisFromMinutes(timeoutMinutes);
    } catch (IllegalArgumentException ex) {
      logger.error(format("Could not convert stateTimeout %s to Integer", stateTimeoutInMinutes), ex);
      return null;
    }
  }

  @Override
  public String commandName() {
    return K8S_APPLY_STATE;
  }

  @Override
  public String stateType() {
    return getStateType();
  }

  @Override
  public void validateParameters(ExecutionContext context) {}

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();

    if (isBlank(filePaths)) {
      invalidFields.put("File paths", "File paths must not be blank");
    }

    return invalidFields;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return k8sStateHelper.executeWrapperWithManifest(this, context);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return k8sStateHelper.handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse executeK8sTask(ExecutionContext context, String activityId) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
        applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES);
    ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

    renderStateVariables(context);

    K8sTaskParameters k8sTaskParameters =
        K8sApplyTaskParameters.builder()
            .activityId(activityId)
            .releaseName(k8sStateHelper.getReleaseName(context, infraMapping))
            .commandName(K8S_APPLY_STATE)
            .k8sTaskType(K8sTaskType.APPLY)
            .timeoutIntervalInMin(Integer.parseInt(stateTimeoutInMinutes))
            .filePaths(filePaths)
            .k8sDelegateManifestConfig(
                k8sStateHelper.createDelegateManifestConfig(context, appManifestMap.get(K8sValuesLocation.Service)))
            .valuesYamlList(k8sStateHelper.getRenderedValuesFiles(appManifestMap, context))
            .skipDryRun(skipDryRun)
            .build();

    return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    Application app = appService.get(context.getAppId());
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    activityService.updateStatus(k8sStateHelper.getActivityId(context), app.getUuid(), executionStatus);

    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    return ExecutionResponse.builder().executionStatus(executionStatus).stateExecutionData(stateExecutionData).build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public List<CommandUnit> commandUnitList(boolean remoteStoreType) {
    List<CommandUnit> applyCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      applyCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.FetchFiles));
    }

    applyCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init));
    applyCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Prepare));
    applyCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Apply));

    if (!skipSteadyStateCheck) {
      applyCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WaitForSteadyState));
    }

    applyCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WrapUp));

    return applyCommandUnits;
  }

  private void renderStateVariables(ExecutionContext context) {
    if (isNotBlank(filePaths)) {
      filePaths = context.renderExpression(filePaths);
    }

    if (isNotBlank(stateTimeoutInMinutes)) {
      stateTimeoutInMinutes = context.renderExpression(stateTimeoutInMinutes);
    }
  }
}
