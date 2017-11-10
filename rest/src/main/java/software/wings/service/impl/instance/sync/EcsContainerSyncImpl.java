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
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResponseMessage;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.instance.sync.request.ContainerFilter;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author rktummala on 09/08/17
 */
public class EcsContainerSyncImpl implements ContainerSync {
  private static final Logger logger = LoggerFactory.getLogger(EcsContainerSyncImpl.class);

  @Inject private AwsHelperService awsHelperService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private InfrastructureMappingService infraMappingService;

  @Override
  public ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest) {
    ContainerFilter filter = syncRequest.getFilter();
    Collection<ContainerDeploymentInfo> containerDeploymentInfoCollection =
        filter.getContainerDeploymentInfoCollection();
    List<ContainerInfo> result = new ArrayList<>();
    for (ContainerDeploymentInfo containerDeploymentInfo : containerDeploymentInfoCollection) {
      try {
        String nextToken = null;
        SettingAttribute settingAttribute = settingsService.get(containerDeploymentInfo.getComputeProviderId());
        List<EncryptedDataDetail> encryptionDetails =
            secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(),
                containerDeploymentInfo.getAppId(), containerDeploymentInfo.getWorkflowExecutionId());
        Validator.notNullCheck("SettingAttribute", settingAttribute);
        AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(settingAttribute, encryptionDetails);
        Validator.notNullCheck("AwsConfig", awsConfig);

        InfrastructureMapping infrastructureMapping =
            infraMappingService.get(containerDeploymentInfo.getAppId(), containerDeploymentInfo.getInfraMappingId());
        Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);
        if (!(infrastructureMapping instanceof EcsInfrastructureMapping)) {
          String msg =
              "Ecs doesn't support infrastructure mapping type :" + infrastructureMapping.getInfraMappingType();
          logger.error(msg);
          throw new WingsException(msg);
        }
        EcsInfrastructureMapping ecsInfraMapping = (EcsInfrastructureMapping) infrastructureMapping;
        String containerSvcName = containerDeploymentInfo.getContainerSvcName();
        String clusterName = containerDeploymentInfo.getClusterName();
        String region = ecsInfraMapping.getRegion();
        List<Task> tasks;

        do {
          ListTasksRequest listTasksRequest = new ListTasksRequest()
                                                  .withCluster(clusterName)
                                                  .withServiceName(containerSvcName)
                                                  .withMaxResults(100)
                                                  .withNextToken(nextToken)
                                                  .withDesiredStatus("RUNNING");
          ListTasksResult listTasksResult;
          try {
            listTasksResult = awsHelperService.listTasks(region, awsConfig, encryptionDetails, listTasksRequest);
          } catch (WingsException ex) {
            // if the cluster / service has been deleted, we need to continue and check the rest of the service names
            List<ResponseMessage> responseMessageList = ex.getResponseMessageList();
            if (!responseMessageList.isEmpty()) {
              ErrorCode errorCode = responseMessageList.get(0).getCode();
              if (errorCode != null) {
                if (ErrorCode.AWS_CLUSTER_NOT_FOUND.getCode().equals(errorCode.getCode())) {
                  logger.info("ECS Cluster not found for service name:" + containerSvcName);
                  continue;
                } else if (ErrorCode.AWS_SERVICE_NOT_FOUND.getCode().equals(errorCode.getCode())) {
                  logger.info("ECS Service not found for service name:" + containerSvcName);
                  continue;
                }
              }
            }
            throw ex;
          }

          if (!listTasksResult.getTaskArns().isEmpty()) {
            DescribeTasksRequest describeTasksRequest =
                new DescribeTasksRequest().withCluster(clusterName).withTasks(listTasksResult.getTaskArns());
            DescribeTasksResult describeTasksResult =
                awsHelperService.describeTasks(region, awsConfig, encryptionDetails, describeTasksRequest);
            tasks = describeTasksResult.getTasks();
            for (Task task : tasks) {
              if (task != null) {
                EcsContainerInfo ecsContainerInfo =
                    EcsContainerInfo.Builder.anEcsContainerInfo()
                        .withClusterName(clusterName)
                        .withTaskDefinitionArn(task.getTaskDefinitionArn())
                        .withTaskArn(task.getTaskArn())
                        .withVersion(task.getVersion())
                        .withStartedAt(task.getStartedAt() == null ? 0L : task.getStartedAt().getTime())
                        .withStartedBy(task.getStartedBy())
                        .withServiceName(containerSvcName)
                        .build();
                result.add(ecsContainerInfo);
              }
            }
          }
          nextToken = listTasksResult.getNextToken();
        } while (nextToken != null);
      } catch (Exception ex) {
        logger.warn("Error while getting instances for container", ex);
      }
    }

    return ContainerSyncResponse.builder().containerInfoList(result).build();
  }
}
