package software.wings.service.impl.aws.manager;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsLambdaHelperServiceManager;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsLambdaHelperServiceManagerImpl implements AwsLambdaHelperServiceManager {
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public AwsLambdaExecuteFunctionResponse executeFunction(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String region, String functionName, String qualifier,
      String payload) {
    return (AwsLambdaExecuteFunctionResponse) executeTask(awsConfig.getAccountId(),
        AwsLambdaExecuteFunctionRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptedDataDetails)
            .region(region)
            .functionName(functionName)
            .qualifier(qualifier)
            .payload(payload)
            .build());
  }

  private AwsResponse executeTask(String accountId, AwsLambdaRequest request) {
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.AWS_LAMBDA_TASK)
                                    .withAccountId(accountId)
                                    .withAppId(GLOBAL_APP_ID)
                                    .withAsync(false)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                                    .withParameters(new Object[] {request})
                                    .build();
    try {
      NotifyResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      if (notifyResponseData instanceof ErrorNotifyResponseData) {
        throw new WingsException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
      }
      return (AwsResponse) notifyResponseData;
    } catch (InterruptedException ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}