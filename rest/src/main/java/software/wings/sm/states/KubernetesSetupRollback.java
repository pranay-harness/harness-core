package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.command.KubernetesSetupParams.KubernetesSetupParamsBuilder.aKubernetesSetupParams;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.StateType.KUBERNETES_SETUP_ROLLBACK;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.KubernetesSetupParams;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.common.Constants;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;

/**
 * Created by brett on 12/18/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesSetupRollback extends ContainerServiceSetup {
  private String commandName = "Setup Replication Controller";

  public KubernetesSetupRollback(String name) {
    super(name, KUBERNETES_SETUP_ROLLBACK.name());
  }

  @Override
  protected ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, Service service,
      ContainerInfrastructureMapping infrastructureMapping, ContainerTask containerTask, String clusterName) {
    ContainerRollbackRequestElement rollbackElement =
        context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_ROLLBACK_REQUEST_PARAM);

    String subscriptionId = null;
    String resourceGroup = null;

    if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
      subscriptionId = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getSubscriptionId();
      resourceGroup = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getResourceGroup();
    }

    String namespace =
        isNotBlank(infrastructureMapping.getNamespace()) ? infrastructureMapping.getNamespace() : "default";

    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;
    boolean useDashInHostname = featureFlagService.isEnabled(FeatureName.USE_DASH_IN_HOSTNAME, app.getAccountId());

    return aKubernetesSetupParams()
        .withAppName(app.getName())
        .withEnvName(env.getName())
        .withServiceName(serviceName)
        .withClusterName(clusterName)
        .withImageDetails(imageDetails)
        .withNamespace(namespace)
        .withContainerTask(containerTask)
        .withControllerNamePrefix(rollbackElement.getControllerNamePrefix())
        .withInfraMappingId(infrastructureMapping.getUuid())
        .withPreviousYamlConfig(rollbackElement.getPreviousYamlConfig())
        .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
        .withRollback(true)
        .withSubscriptionId(subscriptionId)
        .withResourceGroup(resourceGroup)
        .withUseDashInHostname(useDashInHostname)
        .build();
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(ExecutionContext context,
      CommandExecutionResult executionResult, ExecutionStatus status, ImageDetails imageDetails) {
    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
    KubernetesSetupParams setupParams = (KubernetesSetupParams) executionData.getContainerSetupParams();
    ContainerServiceElementBuilder containerServiceElementBuilder =
        ContainerServiceElement.builder()
            .uuid(executionData.getServiceId())
            .image(imageDetails.getName() + ":" + imageDetails.getTag())
            .clusterName(executionData.getClusterName())
            .namespace(setupParams.getNamespace())
            .deploymentType(DeploymentType.KUBERNETES)
            .infraMappingId(setupParams.getInfraMappingId());
    if (executionResult != null) {
      ContainerSetupCommandUnitExecutionData setupExecutionData =
          (ContainerSetupCommandUnitExecutionData) executionResult.getCommandExecutionData();
      if (setupExecutionData != null) {
        containerServiceElementBuilder.name(setupExecutionData.getContainerServiceName());
      }
    }
    return containerServiceElementBuilder.build();
  }

  @Override
  protected boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
        || infrastructureMapping instanceof AzureKubernetesInfrastructureMapping
        || infrastructureMapping instanceof DirectKubernetesInfrastructureMapping;
  }

  @Override
  protected String getDeploymentType() {
    return DeploymentType.KUBERNETES.name();
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }
}
