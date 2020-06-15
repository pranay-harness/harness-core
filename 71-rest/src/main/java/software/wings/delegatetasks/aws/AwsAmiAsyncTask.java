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
import software.wings.service.impl.aws.model.AwsAmiServiceTrafficShiftAlbDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceTrafficShiftAlbSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsAmiHelperServiceDelegate;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class AwsAmiAsyncTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private AwsAmiHelperServiceDelegate awsAmiHelperServiceDelegate;

  public AwsAmiAsyncTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
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
        case EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_SETUP:
          return performAwsAmiServiceTrafficShiftSetup((AwsAmiServiceTrafficShiftAlbSetupRequest) request);

        case EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_DEPLOY:
          return performAwsAmiTrafficShiftDeployment((AwsAmiServiceTrafficShiftAlbDeployRequest) request);

        case EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB:
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

  private AwsResponse performAwsAmiServiceTrafficShiftSetup(AwsAmiServiceTrafficShiftAlbSetupRequest request) {
    ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService, request.getAccountId(),
        request.getAppId(), request.getActivityId(), request.getCommandName());
    return awsAmiHelperServiceDelegate.setUpAmiServiceTrafficShift(request, logCallback);
  }

  private AwsResponse performAwsAmiTrafficShiftDeployment(AwsAmiServiceTrafficShiftAlbDeployRequest request) {
    ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService, request.getAccountId(),
        request.getAppId(), request.getActivityId(), request.getCommandName());
    return awsAmiHelperServiceDelegate.deployAmiServiceTrafficShift(request, logCallback);
  }
}