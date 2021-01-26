package software.wings.delegatetasks.azure.arm.deployment;

import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentManagementGroupContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentResourceGroupContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentSubscriptionContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentTenantContext;

import com.google.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureARMDeploymentService {
  public void deployAtResourceGroupScope(DeploymentResourceGroupContext context) {}

  public void deployAtSubscriptionScope(DeploymentSubscriptionContext context) {}

  public void deployAtManagementGroupScope(DeploymentManagementGroupContext context) {}

  public void deployAtTenantScope(DeploymentTenantContext context) {}
}
