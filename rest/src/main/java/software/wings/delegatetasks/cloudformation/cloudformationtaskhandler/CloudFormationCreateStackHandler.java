package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Singleton;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import lombok.NoArgsConstructor;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse.CloudFormationCommandExecutionResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse.CloudFormationCreateStackResponseBuilder;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@NoArgsConstructor
public class CloudFormationCreateStackHandler extends CloudFormationCommandTaskHandler {
  protected CloudFormationCommandExecutionResponse executeInternal(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details) {
    CloudFormationCreateStackRequest createRequest = (CloudFormationCreateStackRequest) request;
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    encryptionService.decrypt(request.getAwsConfig(), details);
    try {
      executionLogCallback.saveExecutionLog(
          String.format("Starting to create stack with name: %s", createRequest.getStackName()));
      CreateStackRequest createStackRequest = new CreateStackRequest().withStackName(createRequest.getStackName());
      switch (createRequest.getCreateType()) {
        case CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY: {
          createStackRequest.withTemplateBody(createRequest.getData());
          createStackAndWait(createRequest, createStackRequest, builder);
          break;
        }
        case CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL: {
          createStackRequest.withTemplateURL(createRequest.getData());
          createStackAndWait(createRequest, createStackRequest, builder);
          break;
        }
        default: {
          String errorMessage = String.format("Unsupported stack create type: %s", createRequest.getCreateType());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        }
      }
    } catch (Exception ex) {
      String errorMessage =
          String.format("Exception: %s while creating stack: %s", ex.getMessage(), createRequest.getStackName());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }

  private void createStackAndWait(CloudFormationCreateStackRequest createRequest, CreateStackRequest createStackRequest,
      CloudFormationCommandExecutionResponseBuilder builder) {
    executionLogCallback.saveExecutionLog(
        String.format("Sending request to Aws to create stack: %s", createRequest.getStackName()));
    CreateStackResult result = awsHelperService.createStack(
        createRequest.getAwsConfig().getAccessKey(), createRequest.getAwsConfig().getSecretKey(), createStackRequest);
    executionLogCallback.saveExecutionLog(String.format(
        "Returned after sending request to Aws for stack: %s. Now polling for status.", createRequest.getStackName()));
    int timeOutMs = createRequest.getTimeoutInMs() > 0 ? createRequest.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
    long endTime = System.currentTimeMillis() + timeOutMs;
    while (System.currentTimeMillis() < endTime) {
      executionLogCallback.saveExecutionLog("Querying for stack lists");
      DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(result.getStackId());
      List<Stack> stacks = awsHelperService.getAllStacks(createRequest.getAwsConfig().getAccessKey(),
          createRequest.getAwsConfig().getSecretKey(), describeStacksRequest);
      if (stacks.size() < 1) {
        String errorMessage = "Did not get any stacks while querying stacks list";
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        return;
      }
      Stack stack = stacks.get(0);
      switch (stack.getStackStatus()) {
        case "CREATE_COMPLETE": {
          executionLogCallback.saveExecutionLog(
              String.format("Completed creation of stack: %s", createRequest.getStackName()));
          populateInfraMappingPropertiesFromStack(builder, stack);
          return;
        }
        case "CREATE_FAILED": {
          String errorMessage =
              String.format("Error: %s when creating stack: %s", stack.getStackStatusReason(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        case "CREATE_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog(
              String.format("Creation of stack: %s in progress", stack.getStackName()));
          sleep(ofMillis(50));
          continue;
        }
        default: {
          String errorMessage = String.format(
              "Unexpected status: %s while creating stack: %s ", stack.getStackStatus(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
      }
    }
    String errorMessage = String.format("Timing out while creating stack: %s", createRequest.getStackName());
    executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
    builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
  }

  private void populateInfraMappingPropertiesFromStack(
      CloudFormationCommandExecutionResponseBuilder builder, Stack stack) {
    List<Output> outputs = stack.getOutputs();
    if ((outputs == null) || (outputs.isEmpty())) {
      String errorMessage = String.format(
          "No outputs found after creating stack: %s. Infra mapping can't be created", stack.getStackName());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
      return;
    }
    CloudFormationCreateStackResponseBuilder createBuilder = CloudFormationCreateStackResponse.builder();
    createBuilder.stackId(stack.getStackId());
    for (Output output : outputs) {
      switch (output.getOutputKey()) {
        case "region": {
          createBuilder.region(output.getOutputValue());
          executionLogCallback.saveExecutionLog(String.format("Found region for stack: %s", stack.getStackName()));
          break;
        }
        case "vpcs": {
          createBuilder.vpcs(Arrays.asList(output.getOutputValue().split(",")));
          executionLogCallback.saveExecutionLog(String.format("Found VPCs for stack: %s", stack.getStackName()));
          break;
        }
        case "subnets": {
          createBuilder.subnets(Arrays.asList(output.getOutputValue().split(",")));
          executionLogCallback.saveExecutionLog(String.format("Found subnets for stack: %s", stack.getStackName()));
          break;
        }
        case "securityGroups": {
          createBuilder.securityGroups(Arrays.asList(output.getOutputValue().split(",")));
          executionLogCallback.saveExecutionLog(
              String.format("Found security groups for stack: %s", stack.getStackName()));
          break;
        }
        case "tags": {
          Map<String, Object> tagMap = new HashMap<>();
          String[] tokens = output.getOutputValue().split(";");
          Arrays.stream(tokens).forEach(token -> {
            String[] subTokens = token.split(":");
            if (subTokens.length == 2) {
              tagMap.put(subTokens[0], subTokens[1]);
            }
          });
          createBuilder.tagMap(tagMap);
          executionLogCallback.saveExecutionLog(String.format("Found Tag Map for stack: %s", stack.getStackName()));
          break;
        }
        default: {
          executionLogCallback.saveExecutionLog(String.format(
              "Found unknown outputs: %s for stack: %s. Ignored", output.getOutputKey(), stack.getStackName()));
        }
      }
    }
    CloudFormationCreateStackResponse createStackResponse = createBuilder.build();
    if (isEmpty(createStackResponse.getRegion())) {
      String errorMessage =
          String.format("Region not found in the outputs of stack: %s. Region is required", stack.getStackName());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
      return;
    }
    builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS).commandResponse(createStackResponse);
  }
}