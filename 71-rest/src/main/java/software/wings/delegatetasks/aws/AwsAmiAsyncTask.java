package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.FAILED;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.aws.model.AwsAmiRequest;
import software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsAmiHelperServiceDelegate;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsAmiAsyncTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private AwsAmiHelperServiceDelegate awsAmiHelperServiceDelegate;

  public AwsAmiAsyncTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsAmiRequest request = (AwsAmiRequest) parameters[0];
    try {
      AwsAmiRequestType requestType = request.getRequestType();
      switch (requestType) {
        case EXECUTE_AMI_SERVICE_SETUP: {
          AwsAmiServiceSetupRequest awsAmiServiceSetupRequest = (AwsAmiServiceSetupRequest) request;
          ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService,
              awsAmiServiceSetupRequest.getAccountId(), awsAmiServiceSetupRequest.getAppId(),
              awsAmiServiceSetupRequest.getActivityId(), awsAmiServiceSetupRequest.getCommandName());
          return awsAmiHelperServiceDelegate.setUpAmiService(awsAmiServiceSetupRequest, logCallback);
        }
        case EXECUTE_AMI_SERVICE_DEPLOY: {
          AwsAmiServiceDeployRequest deployRequest = (AwsAmiServiceDeployRequest) request;
          ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService, deployRequest.getAccountId(),
              deployRequest.getAppId(), deployRequest.getActivityId(), deployRequest.getCommandName());
          return awsAmiHelperServiceDelegate.deployAmiService(deployRequest, logCallback);
        }
        case EXECUTE_AMI_SWITCH_ROUTE: {
          AwsAmiSwitchRoutesRequest switchRoutesRequest = (AwsAmiSwitchRoutesRequest) request;
          ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService,
              switchRoutesRequest.getAccountId(), switchRoutesRequest.getAppId(), switchRoutesRequest.getActivityId(),
              switchRoutesRequest.getCommandName());
          if (switchRoutesRequest.isRollback()) {
            return awsAmiHelperServiceDelegate.rollbackSwitchAmiRoutes(switchRoutesRequest, logCallback);
          } else {
            return awsAmiHelperServiceDelegate.switchAmiRoutes(switchRoutesRequest, logCallback);
          }
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (Exception ex) {
      return AwsAmiServiceSetupResponse.builder()
          .executionStatus(FAILED)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }
}