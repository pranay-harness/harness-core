package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import lombok.NoArgsConstructor;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationListStacksRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse.CloudFormationCommandExecutionResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationListStacksResponse;
import software.wings.helpers.ext.cloudformation.response.StackSummaryInfo;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;

@Singleton
@NoArgsConstructor
public class CloudFormationListStacksHandler extends CloudFormationCommandTaskHandler {
  protected CloudFormationCommandExecutionResponse executeInternal(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details) {
    CloudFormationListStacksRequest cloudFormationListStacksRequest = (CloudFormationListStacksRequest) request;
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    AwsConfig awsConfig = cloudFormationListStacksRequest.getAwsConfig();
    encryptionService.decrypt(awsConfig, details);
    try {
      DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
      String stackId = cloudFormationListStacksRequest.getStackId();
      if (isNotEmpty(stackId)) {
        describeStacksRequest.withStackName(stackId);
      }
      executionLogCallback.saveExecutionLog("Sending list stacks call to Aws");
      List<Stack> stacks =
          awsHelperService.getAllStacks(awsConfig.getAccessKey(), awsConfig.getSecretKey(), describeStacksRequest);
      executionLogCallback.saveExecutionLog("Completed list stacks call to Aws");
      List<StackSummaryInfo> summaryInfos = new ArrayList<>();
      stacks.stream().forEach(stack -> {
        summaryInfos.add(StackSummaryInfo.builder()
                             .stackId(stack.getStackId())
                             .stackName(stack.getStackName())
                             .stackStatus(stack.getStackStatus())
                             .stackStatusReason(stack.getStackStatusReason())
                             .build());
      });
      builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .commandResponse(CloudFormationListStacksResponse.builder().stackSummaryInfos(summaryInfos).build());
    } catch (Exception ex) {
      String errorMessage = String.format(
          "Exception: %s while getting stacks list: %s", ex.getMessage(), cloudFormationListStacksRequest.getStackId());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }
}