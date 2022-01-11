/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.azure.appservices;

import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_ROLLBACK;

import io.harness.azure.model.AzureConstants;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppRollbackParameters;

import software.wings.beans.Activity;
import software.wings.beans.command.AzureWebAppCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureWebAppSlotRollback extends AzureWebAppSlotSetup {
  public AzureWebAppSlotRollback(String name) {
    super(name, AZURE_WEBAPP_SLOT_ROLLBACK);
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis(ExecutionContext context) {
    return getTimeout(context);
  }

  @Override
  protected boolean supportRemoteManifest() {
    return false;
  }

  @Override
  protected AzureTaskExecutionRequest buildTaskExecutionRequest(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureWebAppRollbackParameters rollbackParameters =
        buildAzureWebAppSlotRollbackParameters(context, azureAppServiceStateData, activity);

    return AzureTaskExecutionRequest.builder()
        .azureConfigDTO(azureVMSSStateHelper.createAzureConfigDTO(azureAppServiceStateData.getAzureConfig()))
        .azureConfigEncryptionDetails(azureAppServiceStateData.getAzureEncryptedDataDetails())
        .azureTaskParameters(rollbackParameters)
        .build();
  }

  private AzureWebAppRollbackParameters buildAzureWebAppSlotRollbackParameters(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureAppServiceSlotSetupContextElement contextElement = readContextElement(context);
    return AzureWebAppRollbackParameters.builder()
        .accountId(azureAppServiceStateData.getApplication().getAccountId())
        .appId(azureAppServiceStateData.getApplication().getAppId())
        .commandName(APP_SERVICE_SLOT_SETUP)
        .activityId(activity.getUuid())
        .appName(contextElement.getWebApp())
        .subscriptionId(azureAppServiceStateData.getSubscriptionId())
        .resourceGroupName(azureAppServiceStateData.getResourceGroup())
        .preDeploymentData(contextElement.getPreDeploymentData())
        .timeoutIntervalInMin(contextElement.getAppServiceSlotSetupTimeOut())
        .build();
  }

  @Override
  @SchemaIgnore
  public String getTargetSlot() {
    return super.getTargetSlot();
  }

  @Override
  @SchemaIgnore
  public String getSlotSteadyStateTimeout() {
    return super.getSlotSteadyStateTimeout();
  }

  @Override
  protected boolean shouldExecute(ExecutionContext context) {
    if (verifyIfContextElementExist(context)) {
      AzureAppServiceSlotSetupContextElement contextElement = readContextElement(context);
      AzureAppServicePreDeploymentData preDeploymentData = contextElement.getPreDeploymentData();
      return preDeploymentData != null;
    }
    return false;
  }

  @Override
  @SchemaIgnore
  public boolean isRollback() {
    return true;
  }

  @Override
  @SchemaIgnore
  public String getAppService() {
    return super.getAppService();
  }

  @Override
  @SchemaIgnore
  public String getDeploymentSlot() {
    return super.getDeploymentSlot();
  }

  @Override
  public String skipMessage() {
    return "No Azure App service setup context element found. Skipping rollback";
  }

  @Override
  public Map<String, String> validateFields() {
    return Collections.emptyMap();
  }

  @Override
  protected ExecutionResponse processDelegateResponse(
      AzureTaskExecutionResponse executionResponse, ExecutionContext context, ExecutionStatus executionStatus) {
    return prepareExecutionResponse(executionResponse, context, executionStatus);
  }

  @Override
  protected List<CommandUnit> commandUnits(boolean isGitFetch) {
    return ImmutableList.of(new AzureWebAppCommandUnit(AzureConstants.STOP_DEPLOYMENT_SLOT),
        new AzureWebAppCommandUnit(AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS),
        new AzureWebAppCommandUnit(AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS),
        new AzureWebAppCommandUnit(AzureConstants.START_DEPLOYMENT_SLOT),
        new AzureWebAppCommandUnit(AzureConstants.SLOT_TRAFFIC_PERCENTAGE),
        new AzureWebAppCommandUnit(AzureConstants.DEPLOYMENT_STATUS));
  }
}
