package software.wings.delegatetasks.azure.arm.deployment;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureBlueprintClient;
import io.harness.azure.client.AzureManagementClient;
import io.harness.azure.context.ARMDeploymentSteadyStateContext;
import io.harness.azure.model.blueprint.assignment.Assignment;
import io.harness.azure.model.blueprint.assignment.AssignmentProvisioningState;
import io.harness.azure.model.blueprint.assignment.operation.AssignmentDeploymentJob;
import io.harness.azure.model.blueprint.assignment.operation.AssignmentOperation;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.serializer.JsonUtils;

import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentBlueprintSteadyStateContext;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.resources.DeploymentOperationProperties;
import com.microsoft.azure.management.resources.implementation.DeploymentOperationInner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class ARMDeploymentSteadyStateChecker {
  @Inject protected TimeLimiter timeLimiter;

  public void waitUntilCompleteWithTimeout(
      ARMDeploymentSteadyStateContext context, AzureManagementClient azureManagementClient, LogCallback logCallback) {
    try {
      Callable<Object> objectCallable = () -> {
        while (true) {
          String provisioningState = azureManagementClient.getARMDeploymentStatus(context);
          logCallback.saveExecutionLog(
              String.format("%nDeployment Status for - [%s] is [%s]", context.getDeploymentName(), provisioningState));

          PagedList<DeploymentOperationInner> deploymentOperations =
              azureManagementClient.getDeploymentOperations(context);
          deploymentOperations.forEach(operation -> logDeploymentOperationStatus(operation, logCallback));

          if (isDeploymentComplete(provisioningState)) {
            if (ARMDeploymentStatus.isSuccess(provisioningState)) {
              logCallback.saveExecutionLog(
                  format("ARM Deployment - [%s] completed successfully", context.getDeploymentName()), LogLevel.INFO,
                  SUCCESS);
              return Boolean.TRUE;
            } else {
              String message = format("ARM Deployment failed for deployment - [%s]", context.getDeploymentName());
              logCallback.saveExecutionLog(message, LogLevel.ERROR, FAILURE);
              throw new InvalidRequestException(message);
            }
          }
          sleep(ofSeconds(context.getStatusCheckIntervalInSeconds()));
        }
      };

      timeLimiter.callWithTimeout(objectCallable, context.getSteadyCheckTimeoutInMinutes(), TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String message = format("Timed out waiting for executing operation deployment - [%s], %n %s",
          context.getDeploymentName(), e.getMessage());
      logCallback.saveExecutionLog(message, LogLevel.ERROR, FAILURE);
      throw new InvalidRequestException(message, e);
    } catch (Exception e) {
      String message = format(
          "Error while waiting for executing operation [%s], %n %s", context.getDeploymentName(), e.getMessage());
      logCallback.saveExecutionLog(message, LogLevel.ERROR, FAILURE);
      throw new InvalidRequestException(message, e);
    }
  }

  private void logDeploymentOperationStatus(DeploymentOperationInner operation, LogCallback logCallback) {
    if (operation.properties() != null && operation.properties().targetResource() != null) {
      DeploymentOperationProperties properties = operation.properties();
      logCallback.saveExecutionLog(String.format("%s - %s: [%s] %s", properties.targetResource().resourceType(),
          properties.targetResource().resourceName(), properties.provisioningState(),
          properties.statusMessage() != null ? properties.statusMessage() : ""));
    }
  }

  private boolean isDeploymentComplete(String provisioningState) {
    return (provisioningState == null) || ARMDeploymentStatus.SUCCEEDED.getStatus().equalsIgnoreCase(provisioningState)
        || ARMDeploymentStatus.FAILED.getStatus().equalsIgnoreCase(provisioningState)
        || ARMDeploymentStatus.CANCELLED.getStatus().equalsIgnoreCase(provisioningState);
  }

  private enum ARMDeploymentStatus {
    ACCEPTED("Accepted"),
    RUNNING("Running"),
    SUCCEEDED("Succeeded"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String status;
    ARMDeploymentStatus(String status) {
      this.status = status;
    }
    public String getStatus() {
      return status;
    }
    public static boolean isSuccess(String status) {
      return SUCCEEDED.getStatus().equalsIgnoreCase(status);
    }
  }

  public void waitUntilCompleteWithTimeout(DeploymentBlueprintSteadyStateContext context,
      AzureBlueprintClient azureBlueprintClient, LogCallback logCallback) {
    try {
      Callable<Object> objectCallable = () -> {
        while (true) {
          String assignmentName = context.getAssignmentName();
          Assignment provisioningState =
              azureBlueprintClient.getAssignment(context.getAzureConfig(), context.getResourceScope(), assignmentName);

          String assignmentProvisioningStatus = getAssignmentProvisioningStatus(provisioningState);
          logCallback.saveExecutionLog(
              String.format("%nDeployment Status for - [%s] is [%s]", assignmentName, assignmentProvisioningStatus));

          if (isDeploymentComplete(assignmentProvisioningStatus)) {
            PagedList<AssignmentOperation> assignmentOperations = azureBlueprintClient.listAssignmentOperations(
                context.getAzureConfig(), context.getResourceScope(), assignmentName);
            logAssignmentOperations(assignmentOperations, logCallback);
            if (AssignmentProvisioningState.SUCCEEDED
                == AssignmentProvisioningState.fromString(assignmentProvisioningStatus)) {
              logCallback.saveExecutionLog(
                  format("Blueprint Deployment - [%s] completed successfully", assignmentName), LogLevel.INFO, SUCCESS);
              return Boolean.TRUE;
            } else {
              String message = format("Blueprint deployment failed, Assignment Name - [%s]", assignmentName);
              throw new InvalidRequestException(message);
            }
          }
          sleep(ofSeconds(context.getStatusCheckIntervalInSeconds()));
        }
      };

      timeLimiter.callWithTimeout(objectCallable, context.getSteadyCheckTimeoutInMinutes(), TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String message = format("Timed out waiting for executing operation deployment - [%s], %n %s",
          context.getAssignmentName(), e.getMessage());
      logCallback.saveExecutionLog(message, LogLevel.ERROR, FAILURE);
      throw new InvalidRequestException(message, e);
    } catch (InvalidRequestException e) {
      logCallback.saveExecutionLog(e.getMessage(), LogLevel.ERROR, FAILURE);
      throw e;
    } catch (Exception e) {
      String message = format(
          "Error while waiting for executing operation [%s], %n %s", context.getAssignmentName(), e.getMessage());
      logCallback.saveExecutionLog(message, LogLevel.ERROR, FAILURE);
      throw new InvalidRequestException(message, e);
    }
  }

  private String getAssignmentProvisioningStatus(final Assignment provisioningState) {
    return provisioningState.getProperties().getProvisioningState().getValue();
  }

  private void logAssignmentOperations(PagedList<AssignmentOperation> assignmentOperations, LogCallback logCallback) {
    assignmentOperations.stream()
        .map(AssignmentOperation::getProperties)
        .map(AssignmentOperation.Properties::getDeployments)
        .flatMap(Stream::of)
        .forEach(assignmentDeploymentJob -> logAssignmentDeploymentJob(assignmentDeploymentJob, logCallback));
  }

  private void logAssignmentDeploymentJob(
      final AssignmentDeploymentJob assignmentDeploymentJob, final LogCallback logCallback) {
    logCallback.saveExecutionLog(format("List Deployment Jobs: [%s]", JsonUtils.asJson(assignmentDeploymentJob)));
  }
}
