package software.wings.delegatetasks.azure.arm.deployment;

import static io.harness.azure.model.AzureConstants.ARM_DEPLOYMENT_STATUS_CHECK_INTERVAL;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import static java.lang.String.format;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureAuthorizationClient;
import io.harness.azure.client.AzureBlueprintClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.azure.model.blueprint.PublishedBlueprint;
import io.harness.azure.model.blueprint.artifact.Artifact;
import io.harness.azure.model.blueprint.assignment.Assignment;
import io.harness.azure.model.blueprint.assignment.WhoIsBlueprintContract;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentBlueprintContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentBlueprintSteadyStateContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.graphrbac.BuiltInRole;
import com.microsoft.azure.management.graphrbac.RoleAssignment;
import com.microsoft.azure.management.network.ResourceIdentityType;
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
  @Inject private AzureAuthorizationClient azureAuthorizationClient;
  @Inject private ARMDeploymentSteadyStateChecker deploymentSteadyStateChecker;

  public void deployBlueprintAtResourceScope(DeploymentBlueprintContext context) {
    LogCallback blueprintDeploymentLogCallback = getBlueprintDeploymentLogCallback(context);
    blueprintDeploymentLogCallback.saveExecutionLog(
        String.format("Starting Blueprint deployment %n- Blueprint Name: [%s] %n- Version ID: [%s]",
            context.getBlueprintName(), context.getVersionId()));

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
      String message = AzureResourceUtility.getAzureCloudExceptionMessage(ex);
      blueprintDeploymentLogCallback.saveExecutionLog(
          format("%nBlueprint deployment failed."), LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw new InvalidRequestException(
          format("Unable to deploy blueprint, scope: %s, Blueprint Name: %s, Assignment Name: %s, Error msg: %s",
              context.getDefinitionResourceScope(), context.getBlueprintName(), context.getAssignment().getName(),
              message));
    }
    blueprintDeploymentLogCallback.saveExecutionLog(
        format("%nBlueprint is assigned successfully."), LogLevel.INFO, SUCCESS);

    performSteadyStateCheck(context);
  }

  private boolean checkExistingAssignments(
      DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    AzureConfig azureConfig = context.getAzureConfig();
    String assignmentResourceScope = context.getAssignmentResourceScope();
    String blueprintId = context.getAssignment().getProperties().getBlueprintId();

    blueprintDeploymentLogCallback.saveExecutionLog(
        String.format("%nStart listing exiting assignments at scope %n- Scope: [%s] %n- Blueprint Id: [%s]",
            assignmentResourceScope, blueprintId));

    Optional<Assignment> optionalAssignment =
        filterAssignmentsByBlueprintId(azureConfig, assignmentResourceScope, blueprintId);

    if (optionalAssignment.isPresent()) {
      blueprintDeploymentLogCallback.saveExecutionLog(
          String.format(
              "Found existing assignment with name - [%s], skip deployment", optionalAssignment.get().getName()),
          LogLevel.INFO, SUCCESS);
      getBlueprintDeploymentSteadyStateLogCallback(context).saveExecutionLog(
          "Skip Deployment steady state check", LogLevel.INFO, SUCCESS);
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
    String resourceScope = context.getDefinitionResourceScope();
    String blueprintName = context.getBlueprintName();
    String versionId = context.getVersionId();

    blueprintDeploymentLogCallback.saveExecutionLog(
        String.format("Start getting already published blueprint with version - [%s]", versionId));

    Optional<PublishedBlueprint> publishedBlueprintVersion =
        getPublishedBlueprintVersion(azureConfig, resourceScope, blueprintName, versionId);

    if (publishedBlueprintVersion.isPresent()) {
      blueprintDeploymentLogCallback.saveExecutionLog(
          String.format("Found already published blueprint with display name - [%s], continue assignment",
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
    String resourceScope = context.getDefinitionResourceScope();
    String blueprintName = context.getBlueprintName();

    blueprintDeploymentLogCallback.saveExecutionLog(
        format("%nStart creating/updating blueprint definition %n- Scope: [%s]", resourceScope));

    azureBlueprintClient.createOrUpdateBlueprint(
        context.getAzureConfig(), resourceScope, blueprintName, context.getBlueprintJSON());

    blueprintDeploymentLogCallback.saveExecutionLog("Blueprint definition created/updated successfully");
  }

  private void createArtifacts(final DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    AzureConfig azureConfig = context.getAzureConfig();
    String resourceScope = context.getDefinitionResourceScope();
    String blueprintName = context.getBlueprintName();
    Map<String, String> artifacts = context.getArtifacts();

    blueprintDeploymentLogCallback.saveExecutionLog("Start creating/updating blueprint artifacts");

    List<Artifact> result = new ArrayList<>();
    artifacts.forEach((artifactName, artifactJSON) -> {
      blueprintDeploymentLogCallback.saveExecutionLog(
          format("Creating/updating artifact with name - [%s]", artifactName));

      Artifact artifact = azureBlueprintClient.createOrUpdateArtifact(
          azureConfig, resourceScope, blueprintName, artifactName, artifactJSON);

      blueprintDeploymentLogCallback.saveExecutionLog("Artifact created/updated successfully");

      result.add(artifact);
    });

    blueprintDeploymentLogCallback.saveExecutionLog(format(
        "All artifacts created/updated successfully. Number of created/updated artifacts - [%s]", result.size()));
  }

  private void publishBlueprintDefinition(
      final DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    String resourceScope = context.getDefinitionResourceScope();
    String blueprintName = context.getBlueprintName();
    String versionId = context.getVersionId();

    blueprintDeploymentLogCallback.saveExecutionLog(
        format("%nStart publishing blueprint definition %n- Blueprint Definition Name: [%s]%n- New Version Id: [%s]",
            blueprintName, versionId));

    azureBlueprintClient.publishBlueprintDefinition(context.getAzureConfig(), resourceScope, blueprintName, versionId);

    blueprintDeploymentLogCallback.saveExecutionLog("Blueprint published successfully");
  }

  private void createAssignment(final DeploymentBlueprintContext context, LogCallback blueprintDeploymentLogCallback) {
    grantAzureBlueprintsSPOwnerRole(context, blueprintDeploymentLogCallback);
    AzureConfig azureConfig = context.getAzureConfig();
    String assignmentResourceScope = context.getAssignmentResourceScope();
    String assignmentName = context.getAssignment().getName();
    String assignmentJSON = context.getAssignmentJSON();

    blueprintDeploymentLogCallback.saveExecutionLog(
        format("%nStart creating assignment %n- Scope: [%s] %n- Assignment Name: [%s]", assignmentResourceScope,
            context.getAssignment().getName()));

    azureBlueprintClient.beginCreateOrUpdateAssignment(
        azureConfig, assignmentResourceScope, assignmentName, assignmentJSON);

    blueprintDeploymentLogCallback.saveExecutionLog("Blueprint assignment request sent successfully");
  }

  private void grantAzureBlueprintsSPOwnerRole(
      final DeploymentBlueprintContext context, final LogCallback blueprintDeploymentLogCallback) {
    AzureConfig azureConfig = context.getAzureConfig();
    String assignmentResourceScope = context.getAssignmentResourceScope();
    String roleAssignmentName = context.getRoleAssignmentName();
    String assignmentSubscriptionId = context.getAssignmentSubscriptionId();
    Assignment assignment = context.getAssignment();
    String assignmentName = assignment.getName();

    blueprintDeploymentLogCallback.saveExecutionLog("Start granting the rights to Azure Blueprints service principal");

    if (ResourceIdentityType.SYSTEM_ASSIGNED == assignment.getIdentity().type()) {
      blueprintDeploymentLogCallback.saveExecutionLog("Assignment is using system-assigned managed identity. "
          + "Owner rights need to be assign to Azure Blueprints service principal");
      blueprintDeploymentLogCallback.saveExecutionLog(
          format("Start getting Azure Blueprints Service Principal details at scope - [%s]", assignmentResourceScope));

      WhoIsBlueprintContract whoIsBlueprintContract =
          azureBlueprintClient.whoIsBlueprint(azureConfig, assignmentResourceScope, assignmentName);
      String objectId = whoIsBlueprintContract.getObjectId();

      blueprintDeploymentLogCallback.saveExecutionLog(format(
          "Azure Blueprints Service Principal details successfully obtained - Azure Blueprints SP Object ID: [%s]",
          objectId));

      blueprintDeploymentLogCallback.saveExecutionLog(format(
          "Start creating role assignment for Azure Blueprints Service Principal on subscription"
              + "%n- Role Assignment Name: [%s] %n- Built In Role: [%s] %n- Azure Blueprints SP Object ID: [%s] %n- Subscription Id: [%s]",
          roleAssignmentName, BuiltInRole.OWNER.toString(), objectId, assignmentSubscriptionId));

      RoleAssignment roleAssignment = azureAuthorizationClient.roleAssignmentAtSubscriptionScope(
          azureConfig, assignmentSubscriptionId, objectId, roleAssignmentName, BuiltInRole.OWNER);

      blueprintDeploymentLogCallback.saveExecutionLog(format(
          "Role assignment successfully created %n- Principal ID: [%s] %n- Role Definition ID: [%s] %n- Scope: [%s]",
          roleAssignment.principalId(), roleAssignment.roleDefinitionId(), roleAssignment.scope()));
    } else {
      blueprintDeploymentLogCallback.saveExecutionLog(format("Assignment is using user-assigned managed identity. "
              + "User is responsible for managing the rights and lifecycle of a user-assigned managed identity. "
              + "%n- Principal ID: [%s]",
          assignment.getIdentity().principalId()));
    }
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
        .assignmentResourceScope(context.getAssignmentResourceScope())
        .assignmentName(context.getAssignment().getName())
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
