/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse.CloudFormationCommandExecutionResponseBuilder;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public abstract class CloudFormationCommandTaskHandler {
  @Inject protected DelegateFileManager delegateFileManager;
  @Inject protected EncryptionService encryptionService;
  @Inject protected AwsHelperService awsHelperService;
  @Inject protected AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;
  @Inject private DelegateLogService delegateLogService;

  protected static final String stackNamePrefix = "HarnessStack-";

  public Optional<Stack> getIfStackExists(String customStackName, String suffix, AwsConfig awsConfig, String region) {
    List<Stack> stacks = awsHelperService.getAllStacks(region, new DescribeStacksRequest(), awsConfig);
    if (isEmpty(stacks)) {
      return Optional.empty();
    }

    if (isNotEmpty(customStackName)) {
      return stacks.stream().filter(stack -> stack.getStackName().equals(customStackName)).findFirst();
    } else {
      return stacks.stream().filter(stack -> stack.getStackName().endsWith(suffix)).findFirst();
    }
  }

  // ten minutes default timeout for polling stack operations
  static final int DEFAULT_TIMEOUT_MS = 10 * 60 * 1000;

  public CloudFormationCommandExecutionResponse execute(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details) {
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(delegateLogService, request.getAccountId(),
        request.getAppId(), request.getActivityId(), request.getCommandName());
    try {
      CloudFormationCommandExecutionResponse result;
      result = executeInternal(request, details, executionLogCallback);
      logStatusMessage(executionLogCallback, result);
      return result;
    } catch (Exception ex) {
      String errorMessage = format("Exception: %s while executing CF task.", ExceptionUtils.getMessage(ex));
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, FAILURE);
      return CloudFormationCommandExecutionResponse.builder()
          .errorMessage(errorMessage)
          .commandExecutionStatus(FAILURE)
          .build();
    }
  }

  private void logStatusMessage(
      ExecutionLogCallback executionLogCallback, CloudFormationCommandExecutionResponse result) {
    final CommandExecutionStatus status = result.getCommandExecutionStatus();
    if (status == SUCCESS) {
      executionLogCallback.saveExecutionLog("Execution finished successfully.", LogLevel.INFO, status);
    } else if (status == FAILURE) {
      executionLogCallback.saveExecutionLog("Execution has been failed.", LogLevel.ERROR, status);
    }
  }

  protected CloudFormationCommandExecutionResponse deleteStack(String stackId, String stackName,
      CloudFormationCommandRequest request, ExecutionLogCallback executionLogCallback) {
    CloudFormationCommandExecutionResponseBuilder builder = CloudFormationCommandExecutionResponse.builder();
    Stack stack = null;
    try {
      long stackEventsTs = System.currentTimeMillis();
      executionLogCallback.saveExecutionLog(String.format("# Starting to delete stack: %s", stackName));
      DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(stackId);
      if (EmptyPredicate.isNotEmpty(request.getCloudFormationRoleArn())) {
        deleteStackRequest.withRoleARN(request.getCloudFormationRoleArn());
      } else {
        executionLogCallback.saveExecutionLog(
            "No specific cloudformation role provided will use the default permissions on delegate.");
      }
      awsHelperService.deleteStack(request.getRegion(), deleteStackRequest, request.getAwsConfig());
      sleep(ofSeconds(30));

      executionLogCallback.saveExecutionLog(
          String.format("# Request to delete stack: %s submitted. Now beginning to poll.", stackName));
      int timeOutMs = request.getTimeoutInMs() > 0 ? request.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
      long endTime = System.currentTimeMillis() + timeOutMs;
      boolean done = false;

      while (System.currentTimeMillis() < endTime && !done) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackId);
        List<Stack> stacks =
            awsHelperService.getAllStacks(request.getRegion(), describeStacksRequest, request.getAwsConfig());
        if (stacks.size() < 1) {
          String message = String.format(
              "# Did not get any stacks with id: %s while querying stacks list. Deletion may have completed",
              stackName);
          executionLogCallback.saveExecutionLog(message);
          builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
          done = true;
          break;
        }
        stack = stacks.get(0);
        stackEventsTs =
            printStackEvents(request.getRegion(), stackId, request.getAwsConfig(), executionLogCallback, stackEventsTs);

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
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
      } else {
        executionLogCallback.saveExecutionLog(
            "Completed deletion of stack", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      }
    } catch (Exception ex) {
      String errorMessage =
          String.format("# Exception: %s while deleting stack: %s", ExceptionUtils.getMessage(ex), stackName);
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }

    printStackResources(request.getRegion(), stackId, request.getAwsConfig(), executionLogCallback);
    return builder.build();
  }

  @VisibleForTesting
  protected long printStackEvents(CloudFormationCommandRequest request, long stackEventsTs, Stack stack,
      ExecutionLogCallback executionLogCallback) {
    List<StackEvent> stackEvents = getStackEvents(request, stack);
    boolean printed = false;
    long currentLatestTs = -1;
    for (StackEvent event : stackEvents) {
      long tsForEvent = event.getTimestamp().getTime();
      if (tsForEvent > stackEventsTs) {
        if (!printed) {
          executionLogCallback.saveExecutionLog("******************** Cloud Formation Events ********************");
          executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
          printed = true;
        }
        executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", event.getResourceStatus(),
            event.getResourceType(), event.getLogicalResourceId(), getStatusReason(event.getResourceStatusReason()),
            event.getPhysicalResourceId()));
        if (currentLatestTs == -1) {
          currentLatestTs = tsForEvent;
        }
      }
    }
    if (currentLatestTs != -1) {
      stackEventsTs = currentLatestTs;
    }
    return stackEventsTs;
  }

  @VisibleForTesting
  protected long printStackEvents(String region, String stackId, AwsConfig awsConfig,
      ExecutionLogCallback executionLogCallback, long stackEventsTs) {
    List<StackEvent> stackEvents = getStackEvents(region, stackId, awsConfig);
    boolean printed = false;
    long currentLatestTs = -1;
    for (StackEvent event : stackEvents) {
      long tsForEvent = event.getTimestamp().getTime();
      if (tsForEvent > stackEventsTs) {
        if (!printed) {
          executionLogCallback.saveExecutionLog("******************** Cloud Formation Events ********************");
          executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
          printed = true;
        }
        executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", event.getResourceStatus(),
            event.getResourceType(), event.getLogicalResourceId(), getStatusReason(event.getResourceStatusReason()),
            event.getPhysicalResourceId()));
        if (currentLatestTs == -1) {
          currentLatestTs = tsForEvent;
        }
      }
    }
    if (currentLatestTs != -1) {
      stackEventsTs = currentLatestTs;
    }
    return stackEventsTs;
  }

  @VisibleForTesting
  protected void printStackResources(
      CloudFormationCommandRequest request, Stack stack, ExecutionLogCallback executionLogCallback) {
    if (stack == null) {
      return;
    }
    List<StackResource> stackResources = getStackResources(request, stack);
    executionLogCallback.saveExecutionLog("******************** Cloud Formation Resources ********************");
    executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
    stackResources.forEach(resource
        -> executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", resource.getResourceStatus(),
            resource.getResourceType(), resource.getLogicalResourceId(),
            getStatusReason(resource.getResourceStatusReason()), resource.getPhysicalResourceId())));
  }

  @VisibleForTesting
  protected void printStackResources(
      String region, String stackId, AwsConfig awsConfig, ExecutionLogCallback executionLogCallback) {
    if (isEmpty(stackId)) {
      return;
    }
    List<StackResource> stackResources = getStackResources(region, stackId, awsConfig);
    executionLogCallback.saveExecutionLog("******************** Cloud Formation Resources ********************");
    executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
    stackResources.forEach(resource
        -> executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", resource.getResourceStatus(),
            resource.getResourceType(), resource.getLogicalResourceId(),
            getStatusReason(resource.getResourceStatusReason()), resource.getPhysicalResourceId())));
  }

  private List<StackResource> getStackResources(CloudFormationCommandRequest request, Stack stack) {
    return awsHelperService.getAllStackResources(request.getRegion(),
        new DescribeStackResourcesRequest().withStackName(stack.getStackName()), request.getAwsConfig());
  }

  private List<StackResource> getStackResources(String region, String stackId, AwsConfig awsConfig) {
    return awsHelperService.getAllStackResources(
        region, new DescribeStackResourcesRequest().withStackName(stackId), awsConfig);
  }

  private List<StackEvent> getStackEvents(CloudFormationCommandRequest request, Stack stack) {
    return awsHelperService.getAllStackEvents(request.getRegion(),
        new DescribeStackEventsRequest().withStackName(stack.getStackName()), request.getAwsConfig());
  }

  private List<StackEvent> getStackEvents(String region, String stackName, AwsConfig awsConfig) {
    return awsHelperService.getAllStackEvents(
        region, new DescribeStackEventsRequest().withStackName(stackName), awsConfig);
  }

  private String getStatusReason(String reason) {
    return isNotEmpty(reason) ? reason : StringUtils.EMPTY;
  }

  protected abstract CloudFormationCommandExecutionResponse executeInternal(CloudFormationCommandRequest request,
      List<EncryptedDataDetail> details, ExecutionLogCallback executionLogCallback);
}
