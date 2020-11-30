package software.wings.sm.states.azure.appservices;

import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_ROLLBACK;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.context.ContextElementType;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppRollbackParameters;

import software.wings.beans.Activity;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.sm.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
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
    return azureVMSSStateHelper.getStateTimeOutFromContext(context, ContextElementType.AZURE_WEBAPP_SETUP);
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
    AzureAppServiceSlotSetupContextElement contextElement = getContextElement(context);
    return AzureWebAppRollbackParameters.builder()
        .accountId(azureAppServiceStateData.getApplication().getAccountId())
        .appId(azureAppServiceStateData.getApplication().getAppId())
        .commandName(APP_SERVICE_SLOT_SETUP)
        .activityId(activity.getUuid())
        .subscriptionId(azureAppServiceStateData.getSubscriptionId())
        .resourceGroupName(azureAppServiceStateData.getResourceGroup())
        .preDeploymentData(contextElement.getPreDeploymentData())
        .build();
  }

  @Override
  @SchemaIgnore
  public String getSlotSteadyStateTimeout() {
    return super.getSlotSteadyStateTimeout();
  }

  @Override
  @SchemaIgnore
  public List<AzureAppServiceApplicationSetting> getApplicationSettings() {
    return super.getApplicationSettings();
  }

  @Override
  @SchemaIgnore
  public List<AzureAppServiceConnectionString> getAppServiceConnectionStrings() {
    return super.getAppServiceConnectionStrings();
  }

  @Override
  protected boolean shouldExecute(ExecutionContext context) {
    if (verifyIfContextElementExist(context)) {
      AzureAppServiceSlotSetupContextElement contextElement = getContextElement(context);
      AzureAppServicePreDeploymentData preDeploymentData = contextElement.getPreDeploymentData();
      return preDeploymentData != null;
    }
    return false;
  }

  @Override
  public boolean isRollback() {
    return true;
  }

  @Override
  public String skipMessage() {
    return "No Azure App service setup context element found. Skipping rollback";
  }
}
