package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;

import com.google.inject.Singleton;

import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationDeleteStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse.CloudFormationCommandExecutionResponseBuilder;

import java.util.List;
import java.util.Optional;

@Singleton
@NoArgsConstructor
public class CloudFormationDeleteStackHandler extends CloudFormationCommandTaskHandler {
  @Override
  protected CloudFormationCommandExecutionResponse executeInternal(CloudFormationCommandRequest request,
      List<EncryptedDataDetail> details, ExecutionLogCallback executionLogCallback) {
    CloudFormationDeleteStackRequest cloudFormationDeleteStackRequest = (CloudFormationDeleteStackRequest) request;
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    AwsConfig awsConfig = cloudFormationDeleteStackRequest.getAwsConfig();
    encryptionService.decrypt(awsConfig, details);
    Optional<Stack> existingStack = getIfStackExists(cloudFormationDeleteStackRequest.getCustomStackName(),
        cloudFormationDeleteStackRequest.getStackNameSuffix(), awsConfig, request.getRegion());
    String stackId;
    String stackName;
    if (existingStack.isPresent()) {
      stackId = existingStack.get().getStackId();
      stackName = existingStack.get().getStackName();
    } else {
      String message = "# No stack found. Returning";
      executionLogCallback.saveExecutionLog(message);
      builder.errorMessage(message).commandExecutionStatus(CommandExecutionStatus.SUCCESS);
      return builder.build();
    }

    try {
      long stackEventsTs = System.currentTimeMillis();
      executionLogCallback.saveExecutionLog(String.format("# Starting to delete stack: %s", stackName));
      DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(stackId);
      awsHelperService.deleteStack(request.getRegion(), deleteStackRequest, awsConfig);
      executionLogCallback.saveExecutionLog(
          String.format("# Request to delete stack: %s submitted. Now beginning to poll.", stackName));
      int timeOutMs = cloudFormationDeleteStackRequest.getTimeoutInMs() > 0
          ? cloudFormationDeleteStackRequest.getTimeoutInMs()
          : DEFAULT_TIMEOUT_MS;
      long endTime = System.currentTimeMillis() + timeOutMs;
      boolean done = false;
      while (System.currentTimeMillis() < endTime && !done) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackId);
        List<Stack> stacks = awsHelperService.getAllStacks(request.getRegion(), describeStacksRequest, awsConfig);
        if (stacks.size() < 1) {
          String message = String.format(
              "# Did not get any stacks with id: %s while querying stacks list. Deletion may have completed",
              stackName);
          executionLogCallback.saveExecutionLog(message);
          builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
          done = true;
          break;
        }
        Stack stack = stacks.get(0);
        switch (stack.getStackStatus()) {
          case "DELETE_COMPLETE": {
            executionLogCallback.saveExecutionLog("# Completed deletion of stack");
            builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
            done = true;
            break;
          }
          case "DELETE_FAILED": {
            String errorMessage = String.format("# Error: %s when deleting stack", stack.getStackStatusReason());
            executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
            builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
            done = true;
            break;
          }
          case "DELETE_IN_PROGRESS": {
            stackEventsTs = printStackEvents(request, stackEventsTs, stack, executionLogCallback);
            break;
          }
          default: {
            String errorMessage = String.format(
                "# Unexpected status: %s while deleting stack: %s ", stack.getStackStatus(), stack.getStackName());
            executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
            builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
            done = true;
            break;
          }
        }
        sleep(ofSeconds(10));
      }
      if (!done) {
        String errorMessage = String.format("# Timing out while deleting stack: %s", stackName);
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
      } else {
        executionLogCallback.saveExecutionLog(
            "Completed deletion of stack", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      }
    } catch (Exception ex) {
      String errorMessage =
          String.format("# Exception: %s while deleting stack: %s", ExceptionUtils.getMessage(ex), stackName);
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }
}
