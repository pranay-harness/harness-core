package software.wings.delegatetasks.azure.arm.deployment;

import static io.harness.azure.model.AzureConstants.ARM_DEPLOYMENT_STATUS_CHECK_INTERVAL;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import static java.lang.String.format;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureBlueprintClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.azure.model.blueprint.PublishedBlueprint;
import io.harness.azure.model.blueprint.artifact.Artifact;
import io.harness.azure.model.blueprint.assignment.Assignment;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentBlueprintContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentBlueprintSteadyStateContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureBlueprintDeploymentService {
  @Inject private AzureBlueprintClient azureBlueprintClient;
  @Inject private ARMDeploymentSteadyStateChecker deploymentSteadyStateChecker;

  public void deployBlueprintAtResourceScope(DeploymentBlueprintContext context) {
    LogCallback blueprintDeploymentLogCallback = getBlueprintDeploymentLogCallback(context);
    blueprintDeploymentLogCallback.saveExecutionLog(String.format(
        "Starting Blueprint deployment for %nResource Scope - [%s], Blueprint name - [%s], Assignment Name - [%s]",
        context.getResourceScope(), context.getBlueprintName(), context.getAssignmentName()));

    if (checkExistingAssignments(context, blueprintDeploymentLogCallback)) {
      return;
    }

    try {
      if (isBlueprintPublished(context, blueprintDeploymentLogCallback)) {
        createAssignment(context, blueprintDeploymentLogCallback);
      } else {
        deployBlueprint(context, blueprintDeploymentLogCallback);
      }
    } catch (Exception ex) {
      blueprintDeploymentLogCallback.saveExecutionLog(
          "Blueprint deployment failed", LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw new InvalidRequestException(
          format("Unable to deploy blueprint, resourceScope: %s, blueprintName: %s, assignmentName: %s",
              context.getResourceScope(), context.getBlueprintName(), context.getAssignmentName()),
          ex);
    }
    blueprintDeploymentLogCallback.saveExecutionLog("Blueprint is assigned successfully", LogLevel.INFO, SUCCESS);

    performSteadyStateCheck(context);
  }

  private boolean checkExistingAssignments(
      DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    AzureConfig azureConfig = context.getAzureConfig();
    String resourceScope = context.getResourceScope();
    String blueprintId = context.getBlueprintId();

    blueprintDeploymentLogCallback.saveExecutionLog(
        String.format("Starting listing exiting assignments, %nResource Scope - [%s] Blueprint Id - [%s]",
            resourceScope, blueprintId));

    Optional<Assignment> optionalAssignment = filterAssignmentsByBlueprintId(azureConfig, resourceScope, blueprintId);

    if (optionalAssignment.isPresent()) {
      blueprintDeploymentLogCallback.saveExecutionLog(
          String.format(
              "Found existing assignment with name - [%s], skip deployment", optionalAssignment.get().getName()),
          LogLevel.INFO, SUCCESS);
      return true;
    } else {
      blueprintDeploymentLogCallback.saveExecutionLog("Not found existing assignment, continue deployment");
    }
    return false;
  }

  private Optional<Assignment> filterAssignmentsByBlueprintId(
      final AzureConfig azureConfig, final String resourceScope, final String blueprintId) {
    PagedList<Assignment> assignments = azureBlueprintClient.listAssignments(azureConfig, resourceScope);
    List<Assignment> assignmentList = new ArrayList<>(assignments);
    return assignmentList.stream()
        .filter(assignment -> blueprintId.equals(assignment.getProperties().getBlueprintId()))
        .findFirst();
  }

  private boolean isBlueprintPublished(DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    AzureConfig azureConfig = context.getAzureConfig();
    String resourceScope = context.getResourceScope();
    String blueprintName = context.getBlueprintName();
    String versionId = context.getVersionId();

    blueprintDeploymentLogCallback.saveExecutionLog(String.format(
        "Starting getting published blueprint, %nResource Scope [%s], Blueprint Name [%s], Version Id - [%s]",
        resourceScope, blueprintName, versionId));

    Optional<PublishedBlueprint> publishedBlueprintVersion =
        getPublishedBlueprintVersion(azureConfig, resourceScope, blueprintName, versionId);

    if (publishedBlueprintVersion.isPresent()) {
      blueprintDeploymentLogCallback.saveExecutionLog(
          String.format("Found published blueprint version with display name - [%s], continue assignment",
              publishedBlueprintVersion.get().getDisplayName()));
      return true;
    } else {
      blueprintDeploymentLogCallback.saveExecutionLog("Not found published blueprint, continue deployment");
    }
    return false;
  }

  private Optional<PublishedBlueprint> getPublishedBlueprintVersion(
      AzureConfig azureConfig, String resourceScope, String blueprintName, String versionId) {
    return Optional.ofNullable(
        azureBlueprintClient.getPublishedBlueprintVersion(azureConfig, resourceScope, blueprintName, versionId));
  }

  private void deployBlueprint(DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    createBlueprintDefinition(context, blueprintDeploymentLogCallback);
    createArtifacts(context, blueprintDeploymentLogCallback);
    publishBlueprintDefinition(context, blueprintDeploymentLogCallback);
    createAssignment(context, blueprintDeploymentLogCallback);
  }

  private void createBlueprintDefinition(
      final DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    String resourceScope = context.getResourceScope();
    String blueprintName = context.getBlueprintName();

    blueprintDeploymentLogCallback.saveExecutionLog(
        format("Start creating blueprint definition, %nResource Scope [%s], Blueprint Name [%s]", resourceScope,
            blueprintName));

    azureBlueprintClient.createOrUpdateBlueprint(
        context.getAzureConfig(), resourceScope, blueprintName, context.getBlueprintJSON());

    blueprintDeploymentLogCallback.saveExecutionLog("Blueprint definition created successfully");
  }

  private void createArtifacts(final DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    AzureConfig azureConfig = context.getAzureConfig();
    String resourceScope = context.getResourceScope();
    String blueprintName = context.getBlueprintName();
    Map<String, String> artifacts = context.getArtifacts();

    blueprintDeploymentLogCallback.saveExecutionLog(
        format("Start creating blueprint artifacts, %nResource Scope [%s], Blueprint Name [%s]", resourceScope,
            blueprintName));

    List<Artifact> result = new ArrayList<>();
    artifacts.forEach((artifactName, artifactJSON) -> {
      blueprintDeploymentLogCallback.saveExecutionLog(
          format("Start creating blueprint artifact, Artifact Name [%s]", artifactName));

      Artifact artifact = azureBlueprintClient.createOrUpdateArtifact(
          azureConfig, resourceScope, blueprintName, artifactName, artifactJSON);

      blueprintDeploymentLogCallback.saveExecutionLog(
          format("Blueprint artifact created successfully, Artifact Name [%s]", artifactName));

      result.add(artifact);
    });

    blueprintDeploymentLogCallback.saveExecutionLog(
        format("Blueprint artifacts created successfully, number of artifacts [%s]", result.size()));
  }

  private void publishBlueprintDefinition(
      final DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    String resourceScope = context.getResourceScope();
    String blueprintName = context.getBlueprintName();
    String versionId = context.getVersionId();

    blueprintDeploymentLogCallback.saveExecutionLog(
        format("Start publishing blueprint definition, %nResource Scope [%s], Blueprint Name [%s], Version Id [%s]",
            resourceScope, blueprintName, versionId));

    azureBlueprintClient.publishBlueprintDefinition(context.getAzureConfig(), resourceScope, blueprintName, versionId);

    blueprintDeploymentLogCallback.saveExecutionLog("Blueprint published successfully");
  }

  private void createAssignment(final DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    AzureConfig azureConfig = context.getAzureConfig();
    String resourceScope = context.getResourceScope();
    String assignmentName = context.getAssignmentName();
    String assignmentJSON = context.getAssignmentJSON();

    blueprintDeploymentLogCallback.saveExecutionLog(format(
        "Start creating assignment, Resource Scope: [%s], Assignment Name: [%s]", resourceScope, assignmentJSON));

    azureBlueprintClient.beginCreateOrUpdateAssignment(azureConfig, resourceScope, assignmentName, assignmentJSON);

    blueprintDeploymentLogCallback.saveExecutionLog("Blueprint assignment request sent successfully");
  }

  private void performSteadyStateCheck(DeploymentBlueprintContext context) {
    LogCallback blueprintDeploymentSteadyStateLogCallback = getBlueprintDeploymentSteadyStateLogCallback(context);

    deploymentSteadyStateChecker.waitUntilCompleteWithTimeout(toBlueprintDeploymentSteadyStateContext(context),
        azureBlueprintClient, blueprintDeploymentSteadyStateLogCallback);
  }

  private DeploymentBlueprintSteadyStateContext toBlueprintDeploymentSteadyStateContext(
      DeploymentBlueprintContext context) {
    return DeploymentBlueprintSteadyStateContext.builder()
        .azureConfig(context.getAzureConfig())
        .resourceScope(context.getResourceScope())
        .assignmentName(context.getAssignmentName())
        .statusCheckIntervalInSeconds(ARM_DEPLOYMENT_STATUS_CHECK_INTERVAL)
        .steadyCheckTimeoutInMinutes(context.getSteadyStateTimeoutInMin())
        .build();
  }

  private LogCallback getBlueprintDeploymentLogCallback(DeploymentBlueprintContext context) {
    return context.getLogStreamingTaskClient().obtainLogCallback(AzureConstants.BLUEPRINT_DEPLOYMENT);
  }

  private LogCallback getBlueprintDeploymentSteadyStateLogCallback(DeploymentBlueprintContext context) {
    return context.getLogStreamingTaskClient().obtainLogCallback(AzureConstants.BLUEPRINT_DEPLOYMENT_STEADY_STATE);
  }
}
