package software.wings.service.impl.aws.manager;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.waiter.ErrorNotifyResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsAsgListAllNamesRequest;
import software.wings.service.impl.aws.model.AwsAsgListAllNamesResponse;
import software.wings.service.impl.aws.model.AwsAsgListDesiredCapacitiesRequest;
import software.wings.service.impl.aws.model.AwsAsgListDesiredCapacitiesResponse;
import software.wings.service.impl.aws.model.AwsAsgListInstancesRequest;
import software.wings.service.impl.aws.model.AwsAsgListInstancesResponse;
import software.wings.service.impl.aws.model.AwsAsgRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsAsgHelperServiceManagerImpl implements AwsAsgHelperServiceManager {
  private static final Logger logger = LoggerFactory.getLogger(AwsAsgHelperServiceManagerImpl.class);
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public List<String> listAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsAsgListAllNamesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build(),
        appId);
    return ((AwsAsgListAllNamesResponse) response).getASgNames();
  }

  @Override
  public List<Instance> listAutoScalingGroupInstances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String autoScalingGroupName, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsAsgListInstancesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .autoScalingGroupName(autoScalingGroupName)
            .build(),
        appId);
    return ((AwsAsgListInstancesResponse) response).getInstances();
  }

  @Override
  public Map<String, Integer> getDesiredCapacitiesOfAsgs(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, List<String> asgs, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsAsgListDesiredCapacitiesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .asgs(asgs)
            .build(),
        appId);
    return ((AwsAsgListDesiredCapacitiesResponse) response).getCapacities();
  }

  private AwsResponse executeTask(String accountId, AwsAsgRequest request, String appId) {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .taskType(TaskType.AWS_ASG_TASK.name())
            .accountId(accountId)
            .appId(isNotEmpty(appId) ? appId : GLOBAL_APP_ID)
            .async(false)
            .tags(isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
            .timeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
            .data(TaskData.builder().parameters(new Object[] {request}).build())
            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
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