package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.FAILED;
import static software.wings.utils.Misc.getMessage;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.impl.aws.model.AwsLambdaRequest.AwsLambdaRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsLambdaAsyncTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private AwsLambdaHelperServiceDelegate awsLambdaHelperServiceDelegate;

  public AwsLambdaAsyncTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsLambdaRequest request = (AwsLambdaRequest) parameters[0];
    try {
      AwsLambdaRequestType requestType = request.getRequestType();
      switch (requestType) {
        case EXECUTE_LAMBA_WF: {
          AwsLambdaExecuteWfRequest awsLambdaExecuteWfRequest = (AwsLambdaExecuteWfRequest) request;
          ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService,
              awsLambdaExecuteWfRequest.getAccountId(), awsLambdaExecuteWfRequest.getAppId(),
              awsLambdaExecuteWfRequest.getActivityId(), awsLambdaExecuteWfRequest.getCommandName());
          return awsLambdaHelperServiceDelegate.executeWf(awsLambdaExecuteWfRequest, logCallback);
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (Exception ex) {
      return AwsLambdaExecuteWfResponse.builder().executionStatus(FAILED).errorMessage(getMessage(ex)).build();
    }
  }
}