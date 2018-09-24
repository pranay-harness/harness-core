package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toMap;

import com.google.inject.Singleton;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import lombok.NoArgsConstructor;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse.CloudFormationCommandExecutionResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse.CloudFormationCreateStackResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo.ExistingStackInfoBuilder;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
public class CloudFormationCreateStackHandler extends CloudFormationCommandTaskHandler {
  protected CloudFormationCommandExecutionResponse executeInternal(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details) {
    AwsConfig awsConfig = request.getAwsConfig();
    encryptionService.decrypt(awsConfig, details);

    CloudFormationCreateStackRequest upsertRequest = (CloudFormationCreateStackRequest) request;

    executionLogCallback.saveExecutionLog("# Checking if stack already exists...");
    Optional<Stack> stackOptional =
        getIfStackExists(upsertRequest.getStackNameSuffix(), awsConfig, request.getRegion());

    if (!stackOptional.isPresent()) {
      executionLogCallback.saveExecutionLog("# Stack does not exist, creating new stack");
      return createStack(upsertRequest);
    } else {
      executionLogCallback.saveExecutionLog("# Stack already exist, updating stack");
      return updateStack(upsertRequest, stackOptional.get());
    }
  }

  private CloudFormationCommandExecutionResponse updateStack(
      CloudFormationCreateStackRequest updateRequest, Stack stack) {
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    try {
      executionLogCallback.saveExecutionLog(
          String.format("# Starting to Update stack with name: %s", stack.getStackName()));
      UpdateStackRequest updateStackRequest = new UpdateStackRequest().withStackName(stack.getStackName());
      if (isNotEmpty(updateRequest.getVariables())) {
        updateStackRequest.withParameters(
            updateRequest.getVariables()
                .entrySet()
                .stream()
                .map(entry -> new Parameter().withParameterKey(entry.getKey()).withParameterValue(entry.getValue()))
                .collect(Collectors.toList()));
      }
      switch (updateRequest.getCreateType()) {
        case CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY: {
          executionLogCallback.saveExecutionLog("# Using Template Body to Update Stack");
          updateStackRequest.withTemplateBody(updateRequest.getData());
          setCapabilitiesOnRequest(updateRequest.getAwsConfig(), updateRequest.getRegion(), updateRequest.getData(),
              "body", updateStackRequest);
          updateStackAndWaitWithEvents(updateRequest, updateStackRequest, builder, stack);
          break;
        }
        case CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL: {
          executionLogCallback.saveExecutionLog("# Using Template Url to Update Stack");
          updateStackRequest.withTemplateURL(updateRequest.getData());
          setCapabilitiesOnRequest(updateRequest.getAwsConfig(), updateRequest.getRegion(), updateRequest.getData(),
              "s3", updateStackRequest);
          updateStackAndWaitWithEvents(updateRequest, updateStackRequest, builder, stack);
          break;
        }
        default: {
          String errorMessage = String.format("# Unsupported stack create type: %s", updateRequest.getCreateType());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        }
      }
    } catch (Exception ex) {
      String errorMessage =
          String.format("# Exception: %s while Updating stack: %s", Misc.getMessage(ex), stack.getStackName());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }

  private CloudFormationCommandExecutionResponse createStack(CloudFormationCreateStackRequest createRequest) {
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    String stackName = stackNamePrefix + createRequest.getStackNameSuffix();
    try {
      executionLogCallback.saveExecutionLog(String.format("# Creating stack with name: %s", stackName));
      CreateStackRequest createStackRequest = new CreateStackRequest().withStackName(stackName);
      if (isNotEmpty(createRequest.getVariables())) {
        createStackRequest.withParameters(
            createRequest.getVariables()
                .entrySet()
                .stream()
                .map(entry -> new Parameter().withParameterKey(entry.getKey()).withParameterValue(entry.getValue()))
                .collect(Collectors.toList()));
      }
      switch (createRequest.getCreateType()) {
        case CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY: {
          executionLogCallback.saveExecutionLog("# Using Template Body to create Stack");
          createStackRequest.withTemplateBody(createRequest.getData());
          setCapabilitiesOnRequest(createRequest.getAwsConfig(), createRequest.getRegion(), createRequest.getData(),
              "body", createStackRequest);
          createStackAndWaitWithEvents(createRequest, createStackRequest, builder);
          break;
        }
        case CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL: {
          executionLogCallback.saveExecutionLog("# Using Template URL to create Stack");
          createStackRequest.withTemplateURL(createRequest.getData());
          setCapabilitiesOnRequest(createRequest.getAwsConfig(), createRequest.getRegion(), createRequest.getData(),
              "s3", createStackRequest);
          createStackAndWaitWithEvents(createRequest, createStackRequest, builder);
          break;
        }
        default: {
          String errorMessage = String.format("Unsupported stack create type: %s", createRequest.getCreateType());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        }
      }
    } catch (Exception ex) {
      String errorMessage = String.format("Exception: %s while creating stack: %s", Misc.getMessage(ex), stackName);
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }

  private void createStackAndWaitWithEvents(CloudFormationCreateStackRequest createRequest,
      CreateStackRequest createStackRequest, CloudFormationCommandExecutionResponseBuilder builder) {
    executionLogCallback.saveExecutionLog(
        String.format("# Calling Aws API to Create stack: %s", createStackRequest.getStackName()));
    long stackEventsTs = System.currentTimeMillis();
    CreateStackResult result = awsHelperService.createStack(createRequest.getRegion(),
        createRequest.getAwsConfig().getAccessKey(), createRequest.getAwsConfig().getSecretKey(), createStackRequest);
    executionLogCallback.saveExecutionLog(String.format(
        "# Create Stack request submitted for stack: %s. Now polling for status.", createStackRequest.getStackName()));
    int timeOutMs = createRequest.getTimeoutInMs() > 0 ? createRequest.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
    long endTime = System.currentTimeMillis() + timeOutMs;
    String errorMsg;
    while (System.currentTimeMillis() < endTime) {
      DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(result.getStackId());
      List<Stack> stacks =
          awsHelperService.getAllStacks(createRequest.getRegion(), createRequest.getAwsConfig().getAccessKey(),
              createRequest.getAwsConfig().getSecretKey(), describeStacksRequest);
      if (stacks.size() < 1) {
        String errorMessage = "# Error: received empty stack list from AWS";
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        return;
      }
      Stack stack = stacks.get(0);
      switch (stack.getStackStatus()) {
        case "CREATE_COMPLETE": {
          executionLogCallback.saveExecutionLog("# Stack creation Successful");
          populateInfraMappingPropertiesFromStack(
              builder, stack, ExistingStackInfo.builder().stackExisted(false).build());
          return;
        }
        case "CREATE_FAILED": {
          errorMsg =
              String.format("# Error: %s while creating stack: %s", stack.getStackStatusReason(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        case "CREATE_IN_PROGRESS": {
          stackEventsTs = printStackEvents(createRequest, stackEventsTs, stack);
          break;
        }
        case "ROLLBACK_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog("Creation of stack failed, Rollback in progress");
          break;
        }
        case "ROLLBACK_FAILED": {
          errorMsg = String.format("# Creation of stack: %s failed, Rollback failed as well.", stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        case "ROLLBACK_COMPLETE": {
          errorMsg = String.format("# Creation of stack: %s failed, Rollback complete", stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        default: {
          String errorMessage = String.format("# Unexpected status: %s while Creating stack ", stack.getStackStatus());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
      }
      sleep(ofSeconds(10));
    }
    String errorMessage = String.format("# Timing out while Creating stack: %s", createStackRequest.getStackName());
    executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
    builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
  }

  private ExistingStackInfo getExistingStackInfo(AwsConfig awsConfig, String region, Stack originalStack) {
    ExistingStackInfoBuilder builder = ExistingStackInfo.builder();
    builder.stackExisted(true);
    builder.oldStackParameters(originalStack.getParameters().stream().collect(
        toMap(Parameter::getParameterKey, Parameter::getParameterValue)));
    builder.oldStackBody(awsCFHelperServiceDelegate.getStackBody(awsConfig, region, originalStack.getStackId()));
    return builder.build();
  }

  private void updateStackAndWaitWithEvents(CloudFormationCreateStackRequest request,
      UpdateStackRequest updateStackRequest, CloudFormationCommandExecutionResponseBuilder builder,
      Stack originalStack) {
    ExistingStackInfo existingStackInfo =
        getExistingStackInfo(request.getAwsConfig(), request.getRegion(), originalStack);
    executionLogCallback.saveExecutionLog(
        String.format("# Calling Aws API to Update stack: %s", originalStack.getStackName()));
    long stackEventsTs = System.currentTimeMillis();
    awsHelperService.updateStack(request.getRegion(), request.getAwsConfig().getAccessKey(),
        request.getAwsConfig().getSecretKey(), updateStackRequest);
    executionLogCallback.saveExecutionLog(String.format(
        "# Update Stack Request submitted for stack: %s. Now polling for status", originalStack.getStackName()));
    int timeOutMs = request.getTimeoutInMs() > 0 ? request.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
    long endTime = System.currentTimeMillis() + timeOutMs;
    while (System.currentTimeMillis() < endTime) {
      DescribeStacksRequest describeStacksRequest =
          new DescribeStacksRequest().withStackName(originalStack.getStackId());
      List<Stack> stacks = awsHelperService.getAllStacks(request.getRegion(), request.getAwsConfig().getAccessKey(),
          request.getAwsConfig().getSecretKey(), describeStacksRequest);
      if (stacks.size() < 1) {
        String errorMessage = "# Error: received empty stack list from AWS";
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        return;
      }
      Stack stack = stacks.get(0);
      switch (stack.getStackStatus()) {
        case "CREATE_COMPLETE":
        case "UPDATE_COMPLETE": {
          executionLogCallback.saveExecutionLog("# Update Successful for stack");
          populateInfraMappingPropertiesFromStack(builder, stack, existingStackInfo);
          return;
        }
        case "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog("Update completed, cleanup in progress");
          break;
        }
        case "UPDATE_ROLLBACK_FAILED": {
          String errorMessage = String.format("# Error: %s when updating stack: %s, Rolling back stack update failed",
              stack.getStackStatusReason(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        case "UPDATE_IN_PROGRESS": {
          stackEventsTs = printStackEvents(request, stackEventsTs, stack);
          break;
        }
        case "UPDATE_ROLLBACK_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog("Update of stack failed, , Rollback in progress");
          builder.commandExecutionStatus(CommandExecutionStatus.FAILURE);
          break;
        }
        case "UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog(
              String.format("Rollback of stack update: %s completed, cleanup in progress", stack.getStackName()));
          break;
        }
        case "UPDATE_ROLLBACK_COMPLETE": {
          String errorMsg = String.format("# Rollback of stack update: %s completed", stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        default: {
          String errorMessage = String.format(
              "# Unexpected status: %s while creating stack: %s ", stack.getStackStatus(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
      }
      sleep(ofSeconds(10));
    }
    String errorMessage = String.format("# Timing out while Updating stack: %s", originalStack.getStackName());
    executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
    builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
  }

  private void populateInfraMappingPropertiesFromStack(
      CloudFormationCommandExecutionResponseBuilder builder, Stack stack, ExistingStackInfo existingStackInfo) {
    CloudFormationCreateStackResponseBuilder createBuilder = CloudFormationCreateStackResponse.builder();
    createBuilder.existingStackInfo(existingStackInfo);
    createBuilder.stackId(stack.getStackId());
    List<Output> outputs = stack.getOutputs();
    if (isNotEmpty(outputs)) {
      createBuilder.cloudFormationOutputMap(
          outputs.stream().collect(toMap(Output::getOutputKey, Output::getOutputValue)));
    }
    createBuilder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
    builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS).commandResponse(createBuilder.build());
    executionLogCallback.saveExecutionLog("# Waiting 30 seconds for instances to come up");
    sleep(ofSeconds(30));
  }

  private void setCapabilitiesOnRequest(
      AwsConfig awsConfig, String region, String data, String type, CreateStackRequest stackRequest) {
    List<String> capabilities = awsCFHelperServiceDelegate.getCapabilities(awsConfig, region, data, type);
    stackRequest.withCapabilities(capabilities);
  }

  private void setCapabilitiesOnRequest(
      AwsConfig awsConfig, String region, String data, String type, UpdateStackRequest stackRequest) {
    List<String> capabilities = awsCFHelperServiceDelegate.getCapabilities(awsConfig, region, data, type);
    stackRequest.withCapabilities(capabilities);
  }
}
