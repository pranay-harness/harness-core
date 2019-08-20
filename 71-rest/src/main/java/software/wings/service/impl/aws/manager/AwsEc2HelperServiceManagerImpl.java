package software.wings.service.impl.aws.manager;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ResourceType;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.ErrorNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class AwsEc2HelperServiceManagerImpl implements AwsEc2HelperServiceManager {
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public void validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ValidateCredentialsRequest.builder().awsConfig(awsConfig).encryptionDetails(encryptionDetails).build(),
        "");
    if (!((AwsEc2ValidateCredentialsResponse) response).isValid()) {
      throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER).addParam("message", "Invalid AWS credentials.");
    }
  }

  @Override
  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListRegionsRequest.builder().awsConfig(awsConfig).encryptionDetails(encryptionDetails).build(), appId);
    return ((AwsEc2ListRegionsResponse) response).getRegions();
  }

  @Override
  public List<String> listVPCs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListVpcsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build(),
        appId);
    return ((AwsEc2ListVpcsResponse) response).getVpcs();
  }

  @Override
  public List<String> listSubnets(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<String> vpcIds, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListSubnetsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .vpcIds(vpcIds)
            .region(region)
            .build(),
        appId);
    return ((AwsEc2ListSubnetsResponse) response).getSubnets();
  }

  @Override
  public List<String> listSGs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<String> vpcIds, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListSGsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .vpcIds(vpcIds)
            .region(region)
            .build(),
        appId);
    return ((AwsEc2ListSGsResponse) response).getSecurityGroups();
  }

  @Override
  public Set<String> listTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String appId, ResourceType resourceType) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListTagsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .resourceType(resourceType.toString())
            .build(),
        appId);
    return ((AwsEc2ListTagsResponse) response).getTags();
  }

  @Override
  public Set<String> listTags(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, ResourceType resourceType) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListTagsRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .resourceType(resourceType.toString())
            .build(),
        GLOBAL_APP_ID);
    return ((AwsEc2ListTagsResponse) response).getTags();
  }

  @Override
  public Set<String> listTags(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId) {
    return listTags(awsConfig, encryptionDetails, region, appId, ResourceType.Instance);
  }

  @Override
  public List<Instance> listEc2Instances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, List<Filter> filters, String appId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsEc2ListInstancesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .filters(filters)
            .build(),
        appId);
    return ((AwsEc2ListInstancesResponse) response).getInstances();
  }

  private AwsResponse executeTask(String accountId, AwsEc2Request request, String appId) {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(accountId)
            .appId(isNotEmpty(appId) ? appId : GLOBAL_APP_ID)
            .async(false)
            .tags(isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
            .data(TaskData.builder()
                      .taskType(TaskType.AWS_EC2_TASK.name())
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