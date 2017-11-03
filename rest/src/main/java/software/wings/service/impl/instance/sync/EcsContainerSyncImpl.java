package software.wings.service.impl.instance.sync;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.AwsConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.request.EcsFilter;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.KmsService;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author rktummala on 09/08/17
 */
public class EcsContainerSyncImpl implements ContainerSync {
  private static final Logger logger = LoggerFactory.getLogger(EcsContainerSyncImpl.class);

  @Inject private AwsHelperService awsHelperService;
  @Inject private SettingsService settingsService;
  @Inject private KmsService kmsService;

  @Override
  public ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest, String workflowId, String appId) {
    EcsFilter filter = (EcsFilter) syncRequest.getFilter();
    String nextToken = null;
    SettingAttribute settingAttribute = settingsService.get(filter.getAwsComputeProviderId());
    List<EncryptedDataDetail> encryptionDetails =
        kmsService.getEncryptionDetails((Encryptable) settingAttribute.getValue(), workflowId, appId);
    Validator.notNullCheck("SettingAttribute", settingAttribute);
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(settingAttribute, encryptionDetails);
    Validator.notNullCheck("AwsConfig", awsConfig);
    List<Task> tasks;
    List<ContainerInfo> result = new ArrayList<>();
    Set<String> serviceNameSet = filter.getServiceNameSet();
    for (String serviceName : serviceNameSet) {
      do {
        ListTasksRequest listTasksRequest = new ListTasksRequest()
                                                .withCluster(filter.getClusterName())
                                                .withServiceName(serviceName)
                                                .withMaxResults(100)
                                                .withNextToken(nextToken)
                                                .withDesiredStatus("RUNNING");
        ListTasksResult listTasksResult;
        try {
          listTasksResult =
              awsHelperService.listTasks(filter.getRegion(), awsConfig, encryptionDetails, listTasksRequest);
        } catch (WingsException ex) {
          // if the cluster / service has been deleted, we need to continue and check the rest of the service names
          List<ResponseMessage> responseMessageList = ex.getResponseMessageList();
          if (!responseMessageList.isEmpty()) {
            ErrorCode errorCode = responseMessageList.get(0).getCode();
            if (errorCode != null) {
              if (ErrorCode.AWS_CLUSTER_NOT_FOUND.getCode().equals(errorCode.getCode())) {
                logger.info("ECS Cluster not found for service name:" + serviceName);
                continue;
              } else if (ErrorCode.AWS_SERVICE_NOT_FOUND.getCode().equals(errorCode.getCode())) {
                logger.info("ECS Service not found for service name:" + serviceName);
                continue;
              }
            }
          }
          throw ex;
        }

        if (!listTasksResult.getTaskArns().isEmpty()) {
          DescribeTasksRequest describeTasksRequest =
              new DescribeTasksRequest().withCluster(filter.getClusterName()).withTasks(listTasksResult.getTaskArns());
          DescribeTasksResult describeTasksResult =
              awsHelperService.describeTasks(filter.getRegion(), awsConfig, encryptionDetails, describeTasksRequest);
          tasks = describeTasksResult.getTasks();
          for (Task task : tasks) {
            if (task != null) {
              EcsContainerInfo ecsContainerInfo =
                  EcsContainerInfo.Builder.anEcsContainerInfo()
                      .withClusterName(filter.getClusterName())
                      .withTaskDefinitionArn(task.getTaskDefinitionArn())
                      .withTaskArn(task.getTaskArn())
                      .withVersion(task.getVersion())
                      .withStartedAt(task.getStartedAt() == null ? 0L : task.getStartedAt().getTime())
                      .withStartedBy(task.getStartedBy())
                      .withServiceName(serviceName)
                      .build();
              result.add(ecsContainerInfo);
            }
          }
        }
        nextToken = listTasksResult.getNextToken();
      } while (nextToken != null);
    }

    return ContainerSyncResponse.builder().containerInfoList(result).build();
  }
}
