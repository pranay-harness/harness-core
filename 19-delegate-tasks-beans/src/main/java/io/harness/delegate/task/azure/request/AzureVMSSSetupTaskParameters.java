package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_SETUP;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSSetupTaskParameters extends AzureVMSSTaskParameters {
  private boolean blueGreen;
  private String vmssNamePrefix;
  private String artifactRevision;
  private String baseVMSSName;
  private String subscriptionId;
  private String resourceGroupName;
  private String userName;
  private String userData;
  private String sshPublicKey;
  private String password;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private int autoScalingSteadyStateVMSSTimeout;
  private boolean useCurrentRunningCount;
  private String vmssAuthType;
  private String vmssDeploymentType;
  private String infraMappingId;

  @Builder
  public AzureVMSSSetupTaskParameters(String appId, String accountId, String activityId, String commandName,
      Integer timeoutIntervalInMin, AzureVMSSTaskType commandType, boolean blueGreen, String vmssNamePrefix,
      String artifactRevision, String baseVMSSName, String subscriptionId, String resourceGroupName, String userName,
      String userData, String sshPublicKey, String password, int minInstances, int maxInstances, int desiredInstances,
      int autoScalingSteadyStateVMSSTimeout, boolean useCurrentRunningCount, String vmssAuthType,
      String vmssDeploymentType, String infraMappingId) {
    super(appId, accountId, activityId, commandName, timeoutIntervalInMin, AZURE_VMSS_SETUP);
    this.blueGreen = blueGreen;
    this.vmssNamePrefix = vmssNamePrefix;
    this.artifactRevision = artifactRevision;
    this.baseVMSSName = baseVMSSName;
    this.subscriptionId = subscriptionId;
    this.resourceGroupName = resourceGroupName;
    this.userName = userName;
    this.userData = userData;
    this.sshPublicKey = sshPublicKey;
    this.password = password;
    this.minInstances = minInstances;
    this.maxInstances = maxInstances;
    this.desiredInstances = desiredInstances;
    this.autoScalingSteadyStateVMSSTimeout = autoScalingSteadyStateVMSSTimeout;
    this.useCurrentRunningCount = useCurrentRunningCount;
    this.vmssAuthType = vmssAuthType;
    this.vmssDeploymentType = vmssDeploymentType;
    this.infraMappingId = infraMappingId;
  }
}
