package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.FAILED;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse;
import software.wings.service.impl.aws.model.AwsLambdaFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.impl.aws.model.AwsLambdaRequest.AwsLambdaRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsLambdaTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AwsLambdaTask.class);
  @Inject private DelegateLogService delegateLogService;
  @Inject private AwsLambdaHelperServiceDelegate awsLambdaHelperServiceDelegate;

  public AwsLambdaTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
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
    AwsLambdaRequestType requestType = request.getRequestType();
    switch (requestType) {
      case EXECUTE_LAMBDA_FUNCTION: {
        try {
          return awsLambdaHelperServiceDelegate.executeFunction((AwsLambdaExecuteFunctionRequest) request);
        } catch (Exception ex) {
          return AwsLambdaExecuteFunctionResponse.builder()
              .executionStatus(ExecutionStatus.FAILED)
              .errorMessage(ExceptionUtils.getMessage(ex))
              .build();
        }
      }
      case EXECUTE_LAMBDA_WF: {
        try {
          AwsLambdaExecuteWfRequest awsLambdaExecuteWfRequest = (AwsLambdaExecuteWfRequest) request;
          ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService,
              awsLambdaExecuteWfRequest.getAccountId(), awsLambdaExecuteWfRequest.getAppId(),
              awsLambdaExecuteWfRequest.getActivityId(), awsLambdaExecuteWfRequest.getCommandName());
          return awsLambdaHelperServiceDelegate.executeWf(awsLambdaExecuteWfRequest, logCallback);
        } catch (Exception ex) {
          return AwsLambdaExecuteWfResponse.builder()
              .executionStatus(FAILED)
              .errorMessage(ExceptionUtils.getMessage(ex))
              .build();
        }
      }
      case LIST_LAMBDA_FUNCTION: {
        try {
          return awsLambdaHelperServiceDelegate.getLambdaFunctions((AwsLambdaFunctionRequest) request);
        } catch (Exception ex) {
          return AwsLambdaFunctionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage(ExceptionUtils.getMessage(ex))
              .build();
        }
      }
      default: { throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER); }
    }
  }
}
