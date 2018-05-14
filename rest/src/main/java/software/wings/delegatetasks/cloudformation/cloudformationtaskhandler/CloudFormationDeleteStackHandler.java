package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Singleton;

import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import lombok.NoArgsConstructor;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationDeleteStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse.CloudFormationCommandExecutionResponseBuilder;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Singleton
@NoArgsConstructor
public class CloudFormationDeleteStackHandler extends CloudFormationCommandTaskHandler {
  protected CloudFormationCommandExecutionResponse executeInternal(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details) {
    CloudFormationDeleteStackRequest cloudFormationDeleteStackRequest = (CloudFormationDeleteStackRequest) request;
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    AwsConfig awsConfig = cloudFormationDeleteStackRequest.getAwsConfig();
    encryptionService.decrypt(awsConfig, details);
    try {
      executionLogCallback.saveExecutionLog(
          String.format("Starting to delete stack with id: %s", cloudFormationDeleteStackRequest.getStackId()));
      DeleteStackRequest deleteStackRequest =
          new DeleteStackRequest().withStackName(cloudFormationDeleteStackRequest.getStackId());
      awsHelperService.deleteStack(awsConfig.getAccessKey(), awsConfig.getSecretKey(), deleteStackRequest);
      executionLogCallback.saveExecutionLog(
          String.format("Request to delete stack with id: %s completed. Now beginning to poll.",
              cloudFormationDeleteStackRequest.getStackId()));
      int timeOutMs = cloudFormationDeleteStackRequest.getTimeoutInMs() > 0
          ? cloudFormationDeleteStackRequest.getTimeoutInMs()
          : DEFAULT_TIMEOUT_MS;
      long endTime = System.currentTimeMillis() + timeOutMs;
      boolean done = false;
      while (System.currentTimeMillis() < endTime && !done) {
        executionLogCallback.saveExecutionLog("Querying for stack lists");
        DescribeStacksRequest describeStacksRequest =
            new DescribeStacksRequest().withStackName(cloudFormationDeleteStackRequest.getStackId());
        List<Stack> stacks =
            awsHelperService.getAllStacks(awsConfig.getAccessKey(), awsConfig.getSecretKey(), describeStacksRequest);
        if (stacks.size() < 1) {
          String message = String.format(
              "Did not get any stacks with id: %s while querying stacks list. Deletion may have completed",
              cloudFormationDeleteStackRequest.getStackId());
          executionLogCallback.saveExecutionLog(message);
          builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
          done = true;
          break;
        }
        Stack stack = stacks.get(0);
        switch (stack.getStackStatus()) {
          case "DELETE_COMPLETE": {
            executionLogCallback.saveExecutionLog(
                String.format("Completed deletion of stack: %s", stack.getStackName()));
            builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
            done = true;
            break;
          }
          case "DELETE_FAILED": {
            String errorMessage =
                String.format("Error: %s when deleting stack: %s", stack.getStackStatusReason(), stack.getStackName());
            executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
            builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
            done = true;
            break;
          }
          case "DELETE_IN_PROGRESS": {
            executionLogCallback.saveExecutionLog(
                String.format("Deletion of stack: %s in progress", stack.getStackName()));
            sleep(ofMillis(50));
            continue;
          }
          default: {
            String errorMessage = String.format(
                "Unexpected status: %s while deleting stack: %s ", stack.getStackStatus(), stack.getStackName());
            executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
            builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
            done = true;
            break;
          }
        }
      }
      if (!done) {
        String errorMessage =
            String.format("Timing out while deleting stack: %s", cloudFormationDeleteStackRequest.getStackId());
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
      }
    } catch (Exception ex) {
      String errorMessage = String.format(
          "Exception: %s while deleting stack: %s", ex.getMessage(), cloudFormationDeleteStackRequest.getStackId());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }
}