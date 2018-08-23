package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.Optional;

public abstract class CloudFormationCommandTaskHandler {
  @Inject protected DelegateFileManager delegateFileManager;
  @Inject protected EncryptionService encryptionService;
  @Inject protected AwsHelperService awsHelperService;
  @Inject protected AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;
  @Inject private DelegateLogService delegateLogService;

  protected ExecutionLogCallback executionLogCallback;
  protected static final String stackNamePrefix = "HarnessStack-";

  protected Optional<Stack> getIfStackExists(String suffix, AwsConfig awsConfig, String region) {
    List<Stack> stacks = awsHelperService.getAllStacks(
        region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), new DescribeStacksRequest());
    if (isEmpty(stacks)) {
      return Optional.empty();
    }

    return stacks.stream().filter(stack -> stack.getStackName().endsWith(suffix)).findFirst();
  }

  // ten minutes default timeout for polling stack operations
  static final int DEFAULT_TIMEOUT_MS = 10 * 60 * 1000;

  public CloudFormationCommandExecutionResponse execute(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details) {
    executionLogCallback = new ExecutionLogCallback(delegateLogService, request.getAccountId(), request.getAppId(),
        request.getActivityId(), request.getCommandName());
    return executeInternal(request, details);
  }

  protected long printStackEvents(CloudFormationCommandRequest request, long stackEventsTs, Stack stack) {
    List<StackEvent> stackEvents = getStackEvents(request, stack);
    boolean printed = false;
    long currentLatestTs = -1;
    for (StackEvent event : stackEvents) {
      long tsForEvent = event.getTimestamp().getTime();
      if (tsForEvent > stackEventsTs) {
        if (!printed) {
          executionLogCallback.saveExecutionLog("******************** Could Formation Events ********************");
          executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
          printed = true;
        }
        executionLogCallback.saveExecutionLog(String.format("[%s] [%s] [%s] [%s] [%s]", event.getResourceStatus(),
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

  private List<StackEvent> getStackEvents(CloudFormationCommandRequest request, Stack stack) {
    return awsHelperService.getAllStackEvents(request.getRegion(), request.getAwsConfig().getAccessKey(),
        request.getAwsConfig().getSecretKey(), new DescribeStackEventsRequest().withStackName(stack.getStackName()));
  }

  private String getStatusReason(String reason) {
    return isNotEmpty(reason) ? reason : StringUtils.EMPTY;
  }

  protected abstract CloudFormationCommandExecutionResponse executeInternal(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details);
}