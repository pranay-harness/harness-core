package software.wings.service.impl.spotinst;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.spotinst.model.SpotInstConstants.defaultSyncSpotinstTimeoutMin;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.spotinst.request.SpotInstGetElastigroupJsonParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupInstancesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupNamesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstGetElastigroupJsonResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupInstancesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupNamesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.spotinst.model.ElastiGroup;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class SpotinstHelperServiceManager {
  @Inject private DelegateService delegateService;

  public List<ElastiGroup> listElastigroups(
      SpotInstConfig spotInstConfig, List<EncryptedDataDetail> encryptionDetails, String appId) {
    SpotInstTaskResponse response =
        executeTask(spotInstConfig.getAccountId(), SpotInstListElastigroupNamesParameters.builder().build(),
            spotInstConfig, encryptionDetails, null, emptyList(), appId);
    return ((SpotInstListElastigroupNamesResponse) response).getElastigroups();
  }

  public String getElastigroupJson(
      SpotInstConfig spotInstConfig, List<EncryptedDataDetail> encryptionDetails, String appId, String elastigroupId) {
    SpotInstTaskResponse response = executeTask(spotInstConfig.getAccountId(),
        SpotInstGetElastigroupJsonParameters.builder().elastigroupId(elastigroupId).build(), spotInstConfig,
        encryptionDetails, null, emptyList(), appId);
    return ((SpotInstGetElastigroupJsonResponse) response).getElastigroupJson();
  }

  public List<Instance> listElastigroupInstances(SpotInstConfig spotinstConfig,
      List<EncryptedDataDetail> spotinstEncryptionDetails, AwsConfig awsConfig,
      List<EncryptedDataDetail> awsEncryptionDetails, String region, String appId, String elastigroupId) {
    SpotInstTaskResponse response = executeTask(spotinstConfig.getAccountId(),
        SpotInstListElastigroupInstancesParameters.builder().awsRegion(region).elastigroupId(elastigroupId).build(),
        spotinstConfig, spotinstEncryptionDetails, awsConfig, awsEncryptionDetails, appId);
    return ((SpotInstListElastigroupInstancesResponse) response).getElastigroupInstances();
  }

  private SpotInstTaskResponse executeTask(String accountId, SpotInstTaskParameters parameters,
      SpotInstConfig spotInstConfig, List<EncryptedDataDetail> spotinstEncryptionDetails, AwsConfig awsConfig,
      List<EncryptedDataDetail> awsEncryptionDetails, String appId) {
    List<String> tagList = null;
    if (awsConfig != null) {
      String tag = awsConfig.getTag();
      if (isNotEmpty(tag)) {
        tagList = singletonList(tag);
      }
    }
    SpotInstCommandRequest request = SpotInstCommandRequest.builder()
                                         .awsConfig(awsConfig)
                                         .awsEncryptionDetails(awsEncryptionDetails)
                                         .spotInstConfig(spotInstConfig)
                                         .spotinstEncryptionDetails(spotinstEncryptionDetails)
                                         .spotInstTaskParameters(parameters)
                                         .build();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .appId(isNotEmpty(appId) ? appId : GLOBAL_APP_ID)
                                    .async(false)
                                    .tags(tagList)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.SPOTINST_COMMAND_TASK.name())
                                              .parameters(new Object[] {request})
                                              .timeout(TimeUnit.MINUTES.toMillis(defaultSyncSpotinstTimeoutMin))
                                              .build())
                                    .build();
    try {
      ResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      if (notifyResponseData instanceof ErrorNotifyResponseData) {
        throw new InvalidRequestException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
      }
      SpotInstTaskExecutionResponse response = (SpotInstTaskExecutionResponse) notifyResponseData;
      if (FAILURE == response.getCommandExecutionStatus()) {
        throw new InvalidRequestException(response.getErrorMessage());
      }
      return response.getSpotInstTaskResponse();
    } catch (InterruptedException ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}