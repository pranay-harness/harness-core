package software.wings.service.impl.aws.manager;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.task.protocol.ResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesRequest;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesResponse;
import software.wings.service.impl.aws.model.AwsEc2ListRegionsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListRegionsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListSGsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListSGsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListSubnetsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListSubnetsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListTagsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListTagsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListVpcsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListVpcsResponse;
import software.wings.service.impl.aws.model.AwsEc2Request;
import software.wings.service.impl.aws.model.AwsEc2ValidateCredentialsRequest;
import software.wings.service.impl.aws.model.AwsEc2ValidateCredentialsResponse;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsEc2HelperServiceManager;
import software.wings.waitnotify.ErrorNotifyResponseData;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsEc2HelperServiceManagerImpl implements AwsEc2HelperServiceManager {
  private static final Logger logger = LoggerFactory.getLogger(AwsEc2HelperServiceManagerImpl.class);
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public void validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ValidateCredentialsRequest.builder().awsConfig(awsConfig).encryptionDetails(encryptionDetails).build());
    if (!((AwsEc2ValidateCredentialsResponse) response).isValid()) {
      throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER).addParam("message", "Invalid AWS credentials.");
    }
  }

  @Override
  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListRegionsRequest.builder().awsConfig(awsConfig).encryptionDetails(encryptionDetails).build());
    return ((AwsEc2ListRegionsResponse) response).getRegions();
  }

  @Override
  public List<String> listVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListVpcsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build());
    return ((AwsEc2ListVpcsResponse) response).getVpcs();
  }

  @Override
  public List<String> listSubnets(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListSubnetsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .vpcIds(vpcIds)
            .region(region)
            .build());
    return ((AwsEc2ListSubnetsResponse) response).getSubnets();
  }

  @Override
  public List<String> listSGs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListSGsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .vpcIds(vpcIds)
            .region(region)
            .build());
    return ((AwsEc2ListSGsResponse) response).getSecurityGroups();
  }

  @Override
  public Set<String> listTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListTagsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build());
    return ((AwsEc2ListTagsResponse) response).getTags();
  }

  @Override
  public List<Instance> listEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<Filter> filters) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListInstancesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .filters(filters)
            .build());
    return ((AwsEc2ListInstancesResponse) response).getInstances();
  }

  private AwsResponse executeTask(String accountId, AwsEc2Request request) {
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.AWS_EC2_TASK)
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