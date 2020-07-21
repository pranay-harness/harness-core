package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.LogLevel;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationListStacksRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse.CloudFormationCommandExecutionResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationListStacksResponse;
import software.wings.helpers.ext.cloudformation.response.StackSummaryInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
public class CloudFormationListStacksHandler extends CloudFormationCommandTaskHandler {
  @Override
  protected CloudFormationCommandExecutionResponse executeInternal(CloudFormationCommandRequest request,
      List<EncryptedDataDetail> details, ExecutionLogCallback executionLogCallback) {
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
      List<Stack> stacks = awsHelperService.getAllStacks(request.getRegion(), describeStacksRequest, awsConfig);
      executionLogCallback.saveExecutionLog("Completed list stacks call to Aws");
      List<StackSummaryInfo> summaryInfos = Collections.emptyList();
      if (isNotEmpty(stacks)) {
        summaryInfos = stacks.stream()
                           .map(stack
                               -> StackSummaryInfo.builder()
                                      .stackId(stack.getStackId())
                                      .stackName(stack.getStackName())
                                      .stackStatus(stack.getStackStatus())
                                      .stackStatusReason(stack.getStackStatusReason())
                                      .build())
                           .collect(Collectors.toList());
      }
      builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .commandResponse(CloudFormationListStacksResponse.builder().stackSummaryInfos(summaryInfos).build());
    } catch (Exception ex) {
      String errorMessage = String.format("Exception: %s while getting stacks list: %s", ExceptionUtils.getMessage(ex),
          cloudFormationListStacksRequest.getStackId());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }
}
