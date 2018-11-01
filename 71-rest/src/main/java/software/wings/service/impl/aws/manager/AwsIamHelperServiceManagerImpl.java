package software.wings.service.impl.aws.manager;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesResponse;
import software.wings.service.impl.aws.model.AwsIamListRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListRolesResponse;
import software.wings.service.impl.aws.model.AwsIamRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsIamHelperServiceManager;
import software.wings.waitnotify.ErrorNotifyResponseData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsIamHelperServiceManagerImpl implements AwsIamHelperServiceManager {
  private static final Logger logger = LoggerFactory.getLogger(AwsIamHelperServiceManagerImpl.class);
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public Map<String, String> listIamRoles(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId) {
    AwsResponse response = getResponse(awsConfig.getAccountId(),
        AwsIamListRolesRequest.builder().awsConfig(awsConfig).encryptionDetails(encryptionDetails).build(), appId);
    return ((AwsIamListRolesResponse) response).getRoles();
  }

  @Override
  public List<String> listIamInstanceRoles(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId) {
    AwsResponse response = getResponse(awsConfig.getAccountId(),
        AwsIamListInstanceRolesRequest.builder().awsConfig(awsConfig).encryptionDetails(encryptionDetails).build(),
        appId);
    return ((AwsIamListInstanceRolesResponse) response).getInstanceRoles();
  }

  private AwsResponse getResponse(String accountId, AwsIamRequest request, String appId) {
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.AWS_IAM_TASK)
                                    .withAccountId(accountId)
                                    .withAppId(isNotEmpty(appId) ? appId : GLOBAL_APP_ID)
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