package software.wings.service.impl.aws.manager;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.HasPredicate.hasSome;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.Cd1SetupFields;

import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesResponse;
import software.wings.service.impl.aws.model.AwsIamListRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListRolesResponse;
import software.wings.service.impl.aws.model.AwsIamRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsIamHelperServiceManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class AwsIamHelperServiceManagerImpl implements AwsIamHelperServiceManager {
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;
  @Inject private AwsHelperServiceManager helper;

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
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(accountId)
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, hasSome(appId) ? appId : GLOBAL_APP_ID)
            .tags(hasSome(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
            .data(TaskData.builder()
                      .async(false)
                      .taskType(TaskType.AWS_IAM_TASK.name())
                      .parameters(new Object[] {request})
                      .timeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                      .build())
            .build();
    try {
      DelegateResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      helper.validateDelegateSuccessForSyncTask(notifyResponseData);
      return (AwsResponse) notifyResponseData;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}
