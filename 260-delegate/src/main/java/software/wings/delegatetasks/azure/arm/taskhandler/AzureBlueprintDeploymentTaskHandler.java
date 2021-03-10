package software.wings.delegatetasks.azure.arm.taskhandler;

import static java.lang.String.format;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;
import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;
import io.harness.delegate.task.azure.arm.request.AzureBlueprintDeploymentParameters;
import io.harness.delegate.task.azure.arm.response.AzureBlueprintDeploymentResponse;
import io.harness.exception.InvalidArgumentsException;

import software.wings.delegatetasks.azure.arm.AbstractAzureARMTaskHandler;
import software.wings.delegatetasks.azure.arm.deployment.AzureBlueprintDeploymentService;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentBlueprintContext;
import software.wings.delegatetasks.azure.arm.deployment.validator.AssignmentJsonValidator;
import software.wings.delegatetasks.azure.arm.deployment.validator.DeploymentBlueprintContextValidator;
import software.wings.delegatetasks.azure.arm.deployment.validator.Validators;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureBlueprintDeploymentTaskHandler extends AbstractAzureARMTaskHandler {
  @Inject private AzureBlueprintDeploymentService azureBlueprintDeploymentService;

  @Override
  protected AzureARMTaskResponse executeTaskInternal(AzureARMTaskParameters azureARMTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureBlueprintDeploymentParameters deploymentParameters =
        (AzureBlueprintDeploymentParameters) azureARMTaskParameters;

    try {
      azureBlueprintDeploymentService.deployBlueprintAtResourceScope(
          toDeploymentBlueprintContext(deploymentParameters, azureConfig, logStreamingTaskClient));
      return AzureBlueprintDeploymentResponse.builder().build();
    } catch (Exception ex) {
      return AzureBlueprintDeploymentResponse.builder().errorMsg(ex.getMessage()).build();
    }
  }

  private DeploymentBlueprintContext toDeploymentBlueprintContext(
      AzureBlueprintDeploymentParameters deploymentTaskParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    String assignmentJson = deploymentTaskParameters.getAssignmentJson();
    String blueprintJson = deploymentTaskParameters.getBlueprintJson();
    Map<String, String> artifacts = AzureResourceUtility.fixArtifactNames(deploymentTaskParameters.getArtifacts());

    Validators.validate(assignmentJson, new AssignmentJsonValidator());

    String blueprintId = AzureResourceUtility.getBlueprintId(assignmentJson);
    String resourceScope = AzureResourceUtility.getResourceScope(blueprintId);
    String versionId = AzureResourceUtility.getVersionId(blueprintId);
    String blueprintName = AzureResourceUtility.getBlueprintName(blueprintId);
    String assignmentName = AzureResourceUtility.getAssignmentName(blueprintName);

    checkBlueprintNameInBlueprintJson(blueprintJson, blueprintName);

    DeploymentBlueprintContext deploymentBlueprintContext =
        DeploymentBlueprintContext.builder()
            .azureConfig(azureConfig)
            .blueprintId(blueprintId)
            .resourceScope(resourceScope)
            .versionId(versionId)
            .blueprintName(blueprintName)
            .blueprintJSON(blueprintJson)
            .artifacts(artifacts)
            .assignmentName(assignmentName)
            .assignmentJSON(assignmentJson)
            .logStreamingTaskClient(logStreamingTaskClient)
            .steadyStateTimeoutInMin(deploymentTaskParameters.getTimeoutIntervalInMin())
            .build();

    Validators.validate(deploymentBlueprintContext, new DeploymentBlueprintContextValidator());
    return deploymentBlueprintContext;
  }

  private void checkBlueprintNameInBlueprintJson(final String blueprintJson, final String blueprintName) {
    Optional<String> blueprintNameFromBlueprintJson =
        AzureResourceUtility.getBlueprintNameFromBlueprintJson(blueprintJson);
    if (blueprintNameFromBlueprintJson.isPresent() && blueprintName.equals(blueprintNameFromBlueprintJson.get())) {
      throw new InvalidArgumentsException(format(
          "Not match blueprint name in blueprint json file and in assignment json properties.blueprintId property. "
              + "Found name in blueprint json: %s, and in assignment json properties.blueprintId: %s",
          blueprintNameFromBlueprintJson.get(), blueprintName));
    }
  }
}
