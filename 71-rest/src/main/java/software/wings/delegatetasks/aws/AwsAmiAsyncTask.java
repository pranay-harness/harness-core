package software.wings.delegatetasks.aws;

import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.utils.Misc.getMessage;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.aws.model.AwsAmiRequest;
import software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsAmiHelperServiceDelegate;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsAmiAsyncTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private AwsAmiHelperServiceDelegate awsAmiHelperServiceDelegate;

  public AwsAmiAsyncTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
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
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (Exception ex) {
      return AwsAmiServiceSetupResponse.builder().executionStatus(FAILED).errorMessage(getMessage(ex)).build();
    }
  }
}