package software.wings.service.impl.aws.manager;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
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
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;

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
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsAsgListAllNamesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build());
    return ((AwsAsgListAllNamesResponse) response).getASgNames();
  }

  @Override
  public List<Instance> listAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsAsgListInstancesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .autoScalingGroupName(autoScalingGroupName)
            .build());
    return ((AwsAsgListInstancesResponse) response).getInstances();
  }

  @Override
  public Map<String, Integer> getDesiredCapacitiesOfAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> asgs) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsAsgListDesiredCapacitiesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .asgs(asgs)
            .build());
    return ((AwsAsgListDesiredCapacitiesResponse) response).getCapacities();
  }

  private AwsResponse executeTask(String accountId, AwsAsgRequest request) {
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.AWS_ASG_TASK)
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