package software.wings.sm.states.provision;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.beans.TaskType.AZURE_ARM_TASK;

import static java.util.Collections.singletonList;

import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.task.azure.arm.AzureARMPreDeploymentData;
import io.harness.delegate.task.azure.arm.request.AzureARMDeploymentParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.Cd1SetupFields;

import software.wings.api.ARMStateExecutionData;
import software.wings.api.arm.ARMPreExistingTemplate;
import software.wings.beans.ARMInfrastructureProvisioner;
import software.wings.beans.Activity;
import software.wings.beans.AzureConfig;
import software.wings.beans.TaskType;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.provision.ARMPreExistingTemplateValidationData.ARMPreExistingTemplateValidationDataBuilder;

import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ARMRollbackState extends ARMProvisionState {
  public ARMRollbackState(String name) {
    super(name);
  }

  @Override
  protected ExecutionResponse executeInternal(ExecutionContext context) {
    ARMPreExistingTemplateValidationData validationData = canPerformRollback(context);
    if (!validationData.isValidData()) {
      return ExecutionResponse.builder()
          .executionStatus(SUCCESS)
          .errorMessage(validationData.getErrorMessage())
          .build();
    }

    ARMPreExistingTemplate preExistingTemplate = validationData.getPreExistingTemplate();
    AzureARMPreDeploymentData preDeploymentData = preExistingTemplate.getPreDeploymentData();
    Activity activity = helper.createARMActivity(context, false, getStateType());

    AzureARMDeploymentParameters taskParams =
        AzureARMDeploymentParameters.builder()
            .appId(context.getAppId())
            .accountId(context.getAccountId())
            .activityId(activity.getUuid())
            .deploymentScope(ARMScopeType.RESOURCE_GROUP)
            .deploymentMode(AzureDeploymentMode.COMPLETE)
            .subscriptionId(preDeploymentData.getSubscriptionId())
            .resourceGroupName(preDeploymentData.getResourceGroup())
            .templateJson(preDeploymentData.getResourceGroupTemplateJson())
            .commandName(ARMStateHelper.AZURE_ARM_COMMAND_UNIT_TYPE)
            .timeoutIntervalInMin(helper.renderTimeout(timeoutExpression, context))
            .rollback(true)
            .build();

    AzureConfig azureConfig = azureVMSSStateHelper.getAzureConfig(cloudProviderId);
    List<EncryptedDataDetail> azureEncryptionDetails =
        azureVMSSStateHelper.getEncryptedDataDetails(context, cloudProviderId);
    AzureConfigDTO azureConfigDTO = azureVMSSStateHelper.createAzureConfigDTO(azureConfig);

    AzureTaskExecutionRequest delegateRequest = AzureTaskExecutionRequest.builder()
                                                    .azureConfigDTO(azureConfigDTO)
                                                    .azureConfigEncryptionDetails(azureEncryptionDetails)
                                                    .azureTaskParameters(taskParams)
                                                    .build();

    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid(generateUuid())
            .accountId(context.getAccountId())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, context.getAppId())
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, context.fetchRequiredEnvironment().getUuid())
            .setupAbstraction(Cd1SetupFields.ENV_TYPE_FIELD, context.getEnvType())
            .data(TaskData.builder()
                      .async(true)
                      .taskType(AZURE_ARM_TASK.name())
                      .parameters(new Object[] {delegateRequest})
                      .timeout(TimeUnit.MINUTES.toMillis(helper.renderTimeout(timeoutExpression, context)))
                      .build())
            .build();
    delegateService.queueTask(delegateTask);
    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(singletonList(delegateTask.getUuid()))
        .stateExecutionData(
            ARMStateExecutionData.builder().taskType(TaskType.AZURE_ARM_TASK).activityId(activity.getUuid()).build())
        .build();
  }

  private ARMPreExistingTemplateValidationData canPerformRollback(ExecutionContext context) {
    ARMPreExistingTemplateValidationDataBuilder validationDataBuilder = ARMPreExistingTemplateValidationData.builder();
    ARMInfrastructureProvisioner provisioner = helper.getProvisioner(context.getAppId(), provisionerId);

    if (provisioner == null || provisioner.getScopeType() == null) {
      validationDataBuilder.isValidData(false);
      validationDataBuilder.errorMessage(
          String.format("No ARM Provisioner or scope found for provionser id - [%s]", provisionerId));
      return validationDataBuilder.build();
    }

    if (ARMScopeType.RESOURCE_GROUP != provisioner.getScopeType()) {
      validationDataBuilder.isValidData(false);
      validationDataBuilder.errorMessage(
          String.format("ARM rollback is supported only for Resource Group scope. Current scope is - [%s]",
              provisioner.getScopeType()));
      return validationDataBuilder.build();
    }

    String key = String.format("%s-%s-%s", provisionerId, subscriptionExpression, resourceGroupExpression);

    ARMPreExistingTemplate preExistingTemplate = helper.getPreExistingTemplate(key, context);

    if (preExistingTemplate == null || preExistingTemplate.getPreDeploymentData() == null) {
      validationDataBuilder.isValidData(false);
      validationDataBuilder.errorMessage(
          String.format("Skipping rollback as no previous template found for resource group - [%s]",
              context.renderExpression(resourceGroupExpression)));
      return validationDataBuilder.build();
    }

    AzureARMPreDeploymentData preDeploymentData = preExistingTemplate.getPreDeploymentData();
    if (isEmpty(preDeploymentData.getSubscriptionId())) {
      validationDataBuilder.isValidData(false);
      validationDataBuilder.errorMessage(
          String.format("Skipping rollback as subscription id is empty for Provisioner - [%s]", provisioner.getName()));
      return validationDataBuilder.build();
    }

    if (isEmpty(preDeploymentData.getResourceGroup())) {
      validationDataBuilder.isValidData(false);
      validationDataBuilder.errorMessage(
          String.format("Skipping rollback as resource group is empty for Provisioner - [%s]", provisioner.getName()));
      return validationDataBuilder.build();
    }

    if (isEmpty(preDeploymentData.getResourceGroupTemplateJson())) {
      validationDataBuilder.isValidData(false);
      validationDataBuilder.errorMessage(
          String.format("Skipping rollback as no previous template found for resource group - [%s]",
              context.renderExpression(resourceGroupExpression)));
      return validationDataBuilder.build();
    }

    validationDataBuilder.isValidData(true);
    validationDataBuilder.preExistingTemplate(preExistingTemplate);
    return validationDataBuilder.build();
  }

  @Override
  @SchemaIgnore
  public String getProvisionerId() {
    return super.getProvisionerId();
  }

  @Override
  @SchemaIgnore
  public String getCloudProviderId() {
    return super.getCloudProviderId();
  }

  @Override
  @SchemaIgnore
  public String getTimeoutExpression() {
    return super.getTimeoutExpression();
  }

  @Override
  @SchemaIgnore
  public AzureDeploymentMode getDeploymentMode() {
    return super.getDeploymentMode();
  }

  @Override
  @SchemaIgnore
  public String getLocationExpression() {
    return super.getLocationExpression();
  }

  @Override
  @SchemaIgnore
  public String getSubscriptionExpression() {
    return super.getSubscriptionExpression();
  }

  @Override
  @SchemaIgnore
  public String getResourceGroupExpression() {
    return super.getResourceGroupExpression();
  }

  @Override
  @SchemaIgnore
  public String getManagementGroupExpression() {
    return super.getManagementGroupExpression();
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }
}
