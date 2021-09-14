/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.k8s;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.sm.StateType.K8S_APPLY;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.logging.CommandExecutionStatus;
import io.harness.tasks.ResponseData;

import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.utils.StateTimeoutUtils;
import software.wings.stencils.DefaultValue;
import software.wings.utils.ApplicationManifestUtils;

import com.github.reinert.jjschema.Attributes;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(_870_CG_ORCHESTRATION)
@OwnedBy(CDP)
public class K8sApplyState extends AbstractK8sState {
  @Inject private AppService appService;
  @Inject private ActivityService activityService;
  @Inject private ApplicationManifestUtils applicationManifestUtils;

  public static final String K8S_APPLY_STATE = "Apply";

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
  @Getter @Setter @Attributes(title = "Skip manifest rendering") private boolean skipRendering;

  @Override
  public Integer getTimeoutMillis() {
    try {
      Integer timeoutMinutes = Integer.valueOf(stateTimeoutInMinutes);
      return StateTimeoutUtils.getTimeoutMillisFromMinutes(timeoutMinutes);
    } catch (IllegalArgumentException ex) {
      log.error(format("Could not convert stateTimeout %s to Integer", stateTimeoutInMinutes), ex);
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
    return executeWrapperWithManifest(this, context, K8sStateHelper.fetchSafeTimeoutInMillis(getTimeoutMillis()));
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse executeK8sTask(ExecutionContext context, String activityId) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = fetchApplicationManifests(context);
    ContainerInfrastructureMapping infraMapping = k8sStateHelper.fetchContainerInfrastructureMapping(context);
    storePreviousHelmDeploymentInfo(context, appManifestMap.get(K8sValuesLocation.Service));

    renderStateVariables(context);

    K8sTaskParameters k8sTaskParameters = K8sApplyTaskParameters.builder()
                                              .activityId(activityId)
                                              .releaseName(fetchReleaseName(context, infraMapping))
                                              .commandName(K8S_APPLY_STATE)
                                              .k8sTaskType(K8sTaskType.APPLY)
                                              .timeoutIntervalInMin(Integer.parseInt(stateTimeoutInMinutes))
                                              .filePaths(filePaths)
                                              .k8sDelegateManifestConfig(createDelegateManifestConfig(
                                                  context, appManifestMap.get(K8sValuesLocation.Service)))
                                              .valuesYamlList(fetchRenderedValuesFiles(appManifestMap, context))
                                              .skipDryRun(skipDryRun)
                                              .skipRendering(skipRendering)
                                              .build();

    return queueK8sDelegateTask(context, k8sTaskParameters, appManifestMap);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    Application app = appService.get(context.getAppId());
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    activityService.updateStatus(fetchActivityId(context), app.getUuid(), executionStatus);

    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    return ExecutionResponse.builder().executionStatus(executionStatus).stateExecutionData(stateExecutionData).build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public List<CommandUnit> commandUnitList(boolean remoteStoreType, String accountId) {
    List<CommandUnit> applyCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.FetchFiles));
    }

    applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Init));
    applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Prepare));
    applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Apply));

    if (!skipSteadyStateCheck) {
      applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.WaitForSteadyState));
    }

    applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.WrapUp));

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
