package software.wings.service.impl.aws.manager;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.task.protocol.ResponseData;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsEcrGetAuthTokenRequest;
import software.wings.service.impl.aws.model.AwsEcrGetAuthTokenResponse;
import software.wings.service.impl.aws.model.AwsEcrGetImageUrlRequest;
import software.wings.service.impl.aws.model.AwsEcrGetImageUrlResponse;
import software.wings.service.impl.aws.model.AwsEcrRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsEcrHelperServiceManager;
import software.wings.waitnotify.ErrorNotifyResponseData;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsEcrHelperServiceManagerImpl implements AwsEcrHelperServiceManager {
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public String getAmazonEcrAuthToken(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String awsAccount, String region) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEcrGetAuthTokenRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .awsAccount(awsAccount)
            .build());
    return ((AwsEcrGetAuthTokenResponse) response).getEcrAuthToken();
  }

  @Override
  public String getEcrImageUrl(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEcrGetImageUrlRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .imageName(imageName)
            .build());
    return ((AwsEcrGetImageUrlResponse) response).getEcrImageUrl();
  }

  private AwsResponse executeTask(String accountId, AwsEcrRequest request) {
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.AWS_ECR_TASK)
                                    .withAccountId(accountId)
                                    .withAppId(GLOBAL_APP_ID)
                                    .withAsync(false)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                                    .withParameters(new Object[] {request})
                                    .build();
    try {
      ResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      if (notifyResponseData instanceof ErrorNotifyResponseData) {
        throw new WingsException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
      }
      return (AwsResponse) notifyResponseData;
    } catch (InterruptedException ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}