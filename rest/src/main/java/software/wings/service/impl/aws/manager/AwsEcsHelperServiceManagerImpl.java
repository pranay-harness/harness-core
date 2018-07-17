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
import software.wings.service.impl.aws.model.AwsEcsListClustersRequest;
import software.wings.service.impl.aws.model.AwsEcsListClustersResponse;
import software.wings.service.impl.aws.model.AwsEcsRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsEcsHelperServiceManager;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsEcsHelperServiceManagerImpl implements AwsEcsHelperServiceManager {
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public List<String> listClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEcsListClustersRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build());
    return ((AwsEcsListClustersResponse) response).getClusters();
  }

  private AwsResponse executeTask(String accountId, AwsEcsRequest request) {
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.AWS_ECS_TASK)
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