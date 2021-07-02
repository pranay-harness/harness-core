package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY;
import static software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse.CloudFormationCommandExecutionResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse.CloudFormationCreateStackResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationRollbackInfo;
import software.wings.helpers.ext.cloudformation.response.CloudFormationRollbackInfo.CloudFormationRollbackInfoBuilder;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo.ExistingStackInfoBuilder;
import software.wings.utils.GitUtilsDelegate;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;

@Singleton
@NoArgsConstructor
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class CloudFormationCreateStackHandler extends CloudFormationCommandTaskHandler {
  @Inject private GitUtilsDelegate gitUtilsDelegate;

  @Override
  protected CloudFormationCommandExecutionResponse executeInternal(CloudFormationCommandRequest request,
      List<EncryptedDataDetail> details, ExecutionLogCallback executionLogCallback) {
    AwsConfig awsConfig = request.getAwsConfig();
    encryptionService.decrypt(awsConfig, details, false);

    CloudFormationCreateStackRequest upsertRequest = (CloudFormationCreateStackRequest) request;

    executionLogCallback.saveExecutionLog("# Checking if stack already exists...");
    Optional<Stack> stackOptional = getIfStackExists(
        upsertRequest.getCustomStackName(), upsertRequest.getStackNameSuffix(), awsConfig, request.getRegion());

    if (!stackOptional.isPresent()) {
      executionLogCallback.saveExecutionLog("# Stack does not exist, creating new stack");
      return createStack(upsertRequest, executionLogCallback);
    } else {
      executionLogCallback.saveExecutionLog("# Stack already exist, updating stack");
      return updateStack(upsertRequest, stackOptional.get(), executionLogCallback);
    }
  }

  private CloudFormationCommandExecutionResponse updateStack(
      CloudFormationCreateStackRequest updateRequest, Stack stack, ExecutionLogCallback executionLogCallback) {
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    try {
      executionLogCallback.saveExecutionLog(format("# Starting to Update stack with name: %s", stack.getStackName()));
      UpdateStackRequest updateStackRequest = new UpdateStackRequest()
                                                  .withStackName(stack.getStackName())
                                                  .withParameters(getCfParams(updateRequest))
                                                  .withCapabilities(updateRequest.getCapabilities())
                                                  .withTags(getCloudformationTags(updateRequest));
      if (EmptyPredicate.isNotEmpty(updateRequest.getCloudFormationRoleArn())) {
        updateStackRequest.withRoleARN(updateRequest.getCloudFormationRoleArn());
      } else {
        executionLogCallback.saveExecutionLog(
            "No specific cloudformation role provided will use the default permissions on delegate.");
      }
      switch (updateRequest.getCreateType()) {
        case CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_GIT: {
          executionLogCallback.saveExecutionLog(format("Fetching template from git url: %s, "
                  + "branch: %s, templatePath: %s, commitId: %s [ ignored branch if commitId is "
                  + "set ]",
              updateRequest.getGitConfig().getRepoUrl(), updateRequest.getGitConfig().getBranch(),
              updateRequest.getGitFileConfig().getFilePath(), updateRequest.getGitFileConfig().getCommitId()));
          setRequestDataFromGit(updateRequest);
          updateStackRequest.withTemplateBody(updateRequest.getData());
          updateStackRequest.withCapabilities(getCapabilities(updateRequest.getAwsConfig(), updateRequest.getRegion(),
              updateRequest.getData(), updateRequest.getCapabilities(), "body"));
          updateStackAndWaitWithEvents(updateRequest, updateStackRequest, builder, stack, executionLogCallback);
          break;
        }
        case CLOUD_FORMATION_STACK_CREATE_BODY: {
          executionLogCallback.saveExecutionLog("# Using Template Body to Update Stack");
          updateStackRequest.withTemplateBody(updateRequest.getData());
          updateStackRequest.withCapabilities(getCapabilities(updateRequest.getAwsConfig(), updateRequest.getRegion(),
              updateRequest.getData(), updateRequest.getCapabilities(), "body"));
          updateStackAndWaitWithEvents(updateRequest, updateStackRequest, builder, stack, executionLogCallback);
          break;
        }
        case CLOUD_FORMATION_STACK_CREATE_URL: {
          executionLogCallback.saveExecutionLog(
              format("# Using Template Url: [%s] to Update Stack", updateRequest.getData()));
          updateStackRequest.withTemplateURL(updateRequest.getData());
          updateStackRequest.withCapabilities(getCapabilities(updateRequest.getAwsConfig(), updateRequest.getRegion(),
              updateRequest.getData(), updateRequest.getCapabilities(), "s3"));
          updateStackAndWaitWithEvents(updateRequest, updateStackRequest, builder, stack, executionLogCallback);
          break;
        }
        default: {
          String errorMessage = format("# Unsupported stack create type: %s", updateRequest.getCreateType());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        }
      }
    } catch (Exception ex) {
      String errorMessage =
          format("# Exception: %s while Updating stack: %s", ExceptionUtils.getMessage(ex), stack.getStackName());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }

  private void setRequestDataFromGit(CloudFormationCreateStackRequest request) {
    GitOperationContext gitOperationContext = gitUtilsDelegate.cloneRepo(
        request.getGitConfig(), request.getGitFileConfig(), request.getSourceRepoEncryptionDetails());
    String templatePathRepo =
        gitUtilsDelegate.resolveAbsoluteFilePath(gitOperationContext, request.getGitFileConfig().getFilePath());
    request.setData(gitUtilsDelegate.getRequestDataFromFile(templatePathRepo));
    request.setCreateType(CLOUD_FORMATION_STACK_CREATE_BODY);
  }

  private CloudFormationCommandExecutionResponse createStack(
      CloudFormationCreateStackRequest createRequest, ExecutionLogCallback executionLogCallback) {
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    String stackName;
    if (isNotEmpty(createRequest.getCustomStackName())) {
      stackName = createRequest.getCustomStackName();
    } else {
      stackName = stackNamePrefix + createRequest.getStackNameSuffix();
    }
    try {
      executionLogCallback.saveExecutionLog(format("# Creating stack with name: %s", stackName));
      CreateStackRequest createStackRequest = new CreateStackRequest()
                                                  .withStackName(stackName)
                                                  .withParameters(getCfParams(createRequest))
                                                  .withCapabilities(createRequest.getCapabilities())
                                                  .withTags(getCloudformationTags(createRequest));
      if (EmptyPredicate.isNotEmpty(createRequest.getCloudFormationRoleArn())) {
        createStackRequest.withRoleARN(createRequest.getCloudFormationRoleArn());
      } else {
        executionLogCallback.saveExecutionLog(
            "No specific cloudformation role provided will use the default permissions on delegate.");
      }
      switch (createRequest.getCreateType()) {
        case CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_GIT: {
          executionLogCallback.saveExecutionLog(format("Fetching template from git url: %s, "
                  + "branch: %s, templatePath: %s, commitId: %s [ ignored branch if commitId is "
                  + "set ] ",
              createRequest.getGitConfig().getRepoUrl(), createRequest.getGitConfig().getBranch(),
              createRequest.getGitFileConfig().getFilePath(), createRequest.getGitFileConfig().getCommitId()));
          setRequestDataFromGit(createRequest);
          createStackRequest.withTemplateBody(createRequest.getData());
          createStackRequest.withCapabilities(getCapabilities(createRequest.getAwsConfig(), createRequest.getRegion(),
              createRequest.getData(), createRequest.getCapabilities(), "body"));
          createStackAndWaitWithEvents(createRequest, createStackRequest, builder, executionLogCallback);
          break;
        }
        case CLOUD_FORMATION_STACK_CREATE_BODY: {
          executionLogCallback.saveExecutionLog("# Using Template Body to create Stack");
          createStackRequest.withTemplateBody(createRequest.getData());
          createStackRequest.withCapabilities(getCapabilities(createRequest.getAwsConfig(), createRequest.getRegion(),
              createRequest.getData(), createRequest.getCapabilities(), "body"));
          createStackAndWaitWithEvents(createRequest, createStackRequest, builder, executionLogCallback);
          break;
        }
        case CLOUD_FORMATION_STACK_CREATE_URL: {
          executionLogCallback.saveExecutionLog(
              format("# Using Template Url: [%s] to Create Stack", createRequest.getData()));
          createStackRequest.withTemplateURL(createRequest.getData());
          createStackRequest.withCapabilities(getCapabilities(createRequest.getAwsConfig(), createRequest.getRegion(),
              createRequest.getData(), createRequest.getCapabilities(), "s3"));
          createStackAndWaitWithEvents(createRequest, createStackRequest, builder, executionLogCallback);
          break;
        }
        default: {
          String errorMessage = format("Unsupported stack create type: %s", createRequest.getCreateType());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        }
      }
    } catch (Exception ex) {
      String errorMessage = format("Exception: %s while creating stack: %s", ExceptionUtils.getMessage(ex), stackName);
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }

  private void createStackAndWaitWithEvents(CloudFormationCreateStackRequest createRequest,
      CreateStackRequest createStackRequest, CloudFormationCommandExecutionResponseBuilder builder,
      ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(
        format("# Calling Aws API to Create stack: %s", createStackRequest.getStackName()));
    long stackEventsTs = System.currentTimeMillis();
    CreateStackResult result =
        awsHelperService.createStack(createRequest.getRegion(), createStackRequest, createRequest.getAwsConfig());
    executionLogCallback.saveExecutionLog(format(
        "# Create Stack request submitted for stack: %s. Now polling for status.", createStackRequest.getStackName()));
    int timeOutMs = createRequest.getTimeoutInMs() > 0 ? createRequest.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
    long endTime = System.currentTimeMillis() + timeOutMs;
    String errorMsg;
    while (System.currentTimeMillis() < endTime) {
      DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(result.getStackId());
      List<Stack> stacks =
          awsHelperService.getAllStacks(createRequest.getRegion(), describeStacksRequest, createRequest.getAwsConfig());
      if (stacks.size() < 1) {
        String errorMessage = "# Error: received empty stack list from AWS";
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        return;
      }
      Stack stack = stacks.get(0);
      switch (stack.getStackStatus()) {
        case "CREATE_COMPLETE": {
          executionLogCallback.saveExecutionLog("# Stack creation Successful");
          populateInfraMappingPropertiesFromStack(builder, stack,
              ExistingStackInfo.builder().stackExisted(false).build(), executionLogCallback, createRequest);
          return;
        }
        case "CREATE_FAILED": {
          errorMsg = format("# Error: %s while creating stack: %s", stack.getStackStatusReason(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        case "CREATE_IN_PROGRESS": {
          stackEventsTs = printStackEvents(createRequest, stackEventsTs, stack, executionLogCallback);
          break;
        }
        case "ROLLBACK_IN_PROGRESS": {
          errorMsg = format("Creation of stack failed, Rollback in progress. Stack Name: %s : Reason: %s",
              stack.getStackName(), stack.getStackStatusReason());
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          break;
        }
        case "ROLLBACK_FAILED": {
          errorMsg = format("# Creation of stack: %s failed, Rollback failed as well. Reason: %s", stack.getStackName(),
              stack.getStackStatusReason());
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        case "ROLLBACK_COMPLETE": {
          errorMsg = format("# Creation of stack: %s failed, Rollback complete", stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        default: {
          String errorMessage = format("# Unexpected status: %s while Creating stack, Status reason: %s",
              stack.getStackStatus(), stack.getStackStatusReason());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
      }
      sleep(ofSeconds(10));
    }
    String errorMessage = format("# Timing out while Creating stack: %s", createStackRequest.getStackName());
    executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
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
      UpdateStackRequest updateStackRequest, CloudFormationCommandExecutionResponseBuilder builder, Stack originalStack,
      ExecutionLogCallback executionLogCallback) {
    ExistingStackInfo existingStackInfo =
        getExistingStackInfo(request.getAwsConfig(), request.getRegion(), originalStack);
    executionLogCallback.saveExecutionLog(
        format("# Calling Aws API to Update stack: %s", originalStack.getStackName()));
    long stackEventsTs = System.currentTimeMillis();
    awsHelperService.updateStack(request.getRegion(), updateStackRequest, request.getAwsConfig());
    executionLogCallback.saveExecutionLog(
        format("# Update Stack Request submitted for stack: %s. Now polling for status", originalStack.getStackName()));
    int timeOutMs = request.getTimeoutInMs() > 0 ? request.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
    long endTime = System.currentTimeMillis() + timeOutMs;
    while (System.currentTimeMillis() < endTime) {
      DescribeStacksRequest describeStacksRequest =
          new DescribeStacksRequest().withStackName(originalStack.getStackId());
      List<Stack> stacks =
          awsHelperService.getAllStacks(request.getRegion(), describeStacksRequest, request.getAwsConfig());
      if (stacks.size() < 1) {
        String errorMessage = "# Error: received empty stack list from AWS";
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        return;
      }
      Stack stack = stacks.get(0);
      switch (stack.getStackStatus()) {
        case "CREATE_COMPLETE":
        case "UPDATE_COMPLETE": {
          executionLogCallback.saveExecutionLog("# Update Successful for stack");
          populateInfraMappingPropertiesFromStack(builder, stack, existingStackInfo, executionLogCallback, request);
          return;
        }
        case "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog("Update completed, cleanup in progress");
          break;
        }
        case "UPDATE_ROLLBACK_FAILED": {
          String errorMessage = format("# Error: %s when updating stack: %s, Rolling back stack update failed",
              stack.getStackStatusReason(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        case "UPDATE_IN_PROGRESS": {
          stackEventsTs = printStackEvents(request, stackEventsTs, stack, executionLogCallback);
          break;
        }
        case "UPDATE_ROLLBACK_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog("Update of stack failed, , Rollback in progress");
          builder.commandExecutionStatus(CommandExecutionStatus.FAILURE);
          break;
        }
        case "UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog(
              format("Rollback of stack update: %s completed, cleanup in progress", stack.getStackName()));
          break;
        }
        case "UPDATE_ROLLBACK_COMPLETE": {
          String errorMsg = format("# Rollback of stack update: %s completed", stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
        default: {
          String errorMessage =
              format("# Unexpected status: %s while creating stack: %s ", stack.getStackStatus(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          return;
        }
      }
      sleep(ofSeconds(10));
    }
    String errorMessage = format("# Timing out while Updating stack: %s", originalStack.getStackName());
    executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
    builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
  }

  private void populateInfraMappingPropertiesFromStack(CloudFormationCommandExecutionResponseBuilder builder,
      Stack stack, ExistingStackInfo existingStackInfo, ExecutionLogCallback executionLogCallback,
      CloudFormationCreateStackRequest cloudFormationCreateStackRequest) {
    CloudFormationCreateStackResponseBuilder createBuilder = CloudFormationCreateStackResponse.builder();
    createBuilder.existingStackInfo(existingStackInfo);
    createBuilder.stackId(stack.getStackId());
    List<Output> outputs = stack.getOutputs();
    if (isNotEmpty(outputs)) {
      createBuilder.cloudFormationOutputMap(
          outputs.stream().collect(toMap(Output::getOutputKey, Output::getOutputValue)));
    }
    createBuilder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
    createBuilder.rollbackInfo(getRollbackInfo(cloudFormationCreateStackRequest));
    builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS).commandResponse(createBuilder.build());
    executionLogCallback.saveExecutionLog("# Waiting 30 seconds for resources to come up");
    sleep(ofSeconds(30));
  }

  private CloudFormationRollbackInfo getRollbackInfo(
      CloudFormationCreateStackRequest cloudFormationCreateStackRequest) {
    CloudFormationRollbackInfoBuilder builder = CloudFormationRollbackInfo.builder();

    builder.cloudFormationRoleArn(cloudFormationCreateStackRequest.getCloudFormationRoleArn());
    if (CLOUD_FORMATION_STACK_CREATE_URL.equals(cloudFormationCreateStackRequest.getCreateType())) {
      builder.url(cloudFormationCreateStackRequest.getData());
    } else {
      // handles the case of both Git and body
      builder.body(cloudFormationCreateStackRequest.getData());
    }
    builder.region(cloudFormationCreateStackRequest.getRegion());
    builder.customStackName(cloudFormationCreateStackRequest.getCustomStackName());
    List<NameValuePair> variables = newArrayList();
    if (isNotEmpty(cloudFormationCreateStackRequest.getVariables())) {
      for (Entry<String, String> variable : cloudFormationCreateStackRequest.getVariables().entrySet()) {
        variables.add(new NameValuePair(variable.getKey(), variable.getValue(), Type.TEXT.name()));
      }
    }
    if (isNotEmpty(cloudFormationCreateStackRequest.getEncryptedVariables())) {
      for (Entry<String, EncryptedDataDetail> encVariable :
          cloudFormationCreateStackRequest.getEncryptedVariables().entrySet()) {
        variables.add(new NameValuePair(
            encVariable.getKey(), encVariable.getValue().getEncryptedData().getUuid(), Type.ENCRYPTED_TEXT.name()));
      }
    }
    builder.variables(variables);
    return builder.build();
  }

  @VisibleForTesting
  Set<String> getCapabilities(
      AwsConfig awsConfig, String region, String data, List<String> userDefinedCapabilities, String type) {
    List<String> capabilities = awsCFHelperServiceDelegate.getCapabilities(awsConfig, region, data, type);
    Set<String> allCapabilities = new HashSet<>();

    if (isNotEmpty(userDefinedCapabilities)) {
      allCapabilities.addAll(userDefinedCapabilities);
    }

    allCapabilities.addAll(capabilities);
    return allCapabilities;
  }

  private List<Parameter> getCfParams(CloudFormationCreateStackRequest cloudFormationCreateStackRequest)
      throws Exception {
    List<Parameter> allParams = newArrayList();
    if (isNotEmpty(cloudFormationCreateStackRequest.getVariables())) {
      cloudFormationCreateStackRequest.getVariables().forEach(
          (key, value) -> allParams.add(new Parameter().withParameterKey(key).withParameterValue(value)));
    }
    if (isNotEmpty(cloudFormationCreateStackRequest.getEncryptedVariables())) {
      for (Map.Entry<String, EncryptedDataDetail> entry :
          cloudFormationCreateStackRequest.getEncryptedVariables().entrySet()) {
        allParams.add(
            new Parameter()
                .withParameterKey(entry.getKey())
                .withParameterValue(String.valueOf(encryptionService.getDecryptedValue(entry.getValue(), false))));
      }
    }
    return allParams;
  }

  @VisibleForTesting
  List<Tag> getCloudformationTags(CloudFormationCreateStackRequest cloudFormationCreateStackRequest)
      throws IOException {
    List<Tag> tags = null;
    if (isNotEmpty(cloudFormationCreateStackRequest.getTags())) {
      ObjectMapper mapper = new ObjectMapper();
      tags = Arrays.asList(mapper.readValue(cloudFormationCreateStackRequest.getTags(), Tag[].class));
    }
    return tags;
  }
}