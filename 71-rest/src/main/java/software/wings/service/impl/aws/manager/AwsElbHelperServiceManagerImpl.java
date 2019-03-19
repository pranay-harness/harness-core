package software.wings.service.impl.aws.manager;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.waiter.ErrorNotifyResponseData;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsElbListAppElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListAppElbsResponse;
import software.wings.service.impl.aws.model.AwsElbListClassicElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListClassicElbsResponse;
import software.wings.service.impl.aws.model.AwsElbListElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListListenerRequest;
import software.wings.service.impl.aws.model.AwsElbListListenerResponse;
import software.wings.service.impl.aws.model.AwsElbListNetworkElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListTargetGroupsRequest;
import software.wings.service.impl.aws.model.AwsElbListTargetGroupsResponse;
import software.wings.service.impl.aws.model.AwsElbRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsElbHelperServiceManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsElbHelperServiceManagerImpl implements AwsElbHelperServiceManager {
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public List<String> listClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsElbListClassicElbsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build(),
        appId);
    return ((AwsElbListClassicElbsResponse) response).getClassicElbs();
  }

  @Override
  public List<String> listApplicationLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsElbListAppElbsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build(),
        appId);
    return ((AwsElbListAppElbsResponse) response).getAppElbs();
  }

  @Override
  public List<String> listElasticLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsElbListElbsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build(),
        appId);
    return ((AwsElbListAppElbsResponse) response).getAppElbs();
  }

  @Override
  public List<String> listNetworkLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsElbListNetworkElbsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build(),
        appId);
    return ((AwsElbListAppElbsResponse) response).getAppElbs();
  }

  @Override
  public Map<String, String> listTargetGroupsForAlb(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String loadBalancerName, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsElbListTargetGroupsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .loadBalancerName(loadBalancerName)
            .build(),
        appId);
    return ((AwsElbListTargetGroupsResponse) response).getTargetGroups();
  }

  @Override
  public List<AwsElbListener> listListenersForElb(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String loadBalancerName, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsElbListListenerRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .loadBalancerName(loadBalancerName)
            .build(),
        appId);
    return ((AwsElbListListenerResponse) response).getAwsElbListeners();
  }

  private AwsResponse executeTask(String accountId, AwsElbRequest request, String appId) {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(accountId)
            .appId(isNotEmpty(appId) ? appId : GLOBAL_APP_ID)
            .async(false)
            .tags(isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
            .data(TaskData.builder()
                      .taskType(TaskType.AWS_ELB_TASK.name())
                      .parameters(new Object[] {request})
                      .timeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                      .build())
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