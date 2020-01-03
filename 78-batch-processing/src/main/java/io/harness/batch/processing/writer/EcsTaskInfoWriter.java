package io.harness.batch.processing.writer;

import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsTaskDescription;
import io.harness.event.payloads.EcsTaskInfo;
import io.harness.event.payloads.ReservedResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentSummary;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.instance.HarnessServiceInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
public class EcsTaskInfoWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    logger.info("Published batch size is EcsTaskInfoWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.ECS_TASK_INFO))
        .forEach(publishedMessage -> {
          EcsTaskInfo ecsTaskInfo = (EcsTaskInfo) publishedMessage.getMessage();
          logger.debug("Message {} ", ecsTaskInfo);
          EcsTaskDescription ecsTaskDescription = ecsTaskInfo.getEcsTaskDescription();
          ReservedResource ecsTaskResource = ecsTaskInfo.getEcsTaskResource();
          String accountId = publishedMessage.getAccountId();
          String taskId = getIdFromArn(ecsTaskDescription.getTaskArn());
          String clusterName = getIdFromArn(ecsTaskDescription.getClusterArn());
          String clusterId = ecsTaskDescription.getClusterId();
          String settingId = ecsTaskDescription.getSettingId();

          InstanceData instanceData = fetchActiveInstanceData(accountId, taskId);
          InstanceType instanceType = getInstanceType(ecsTaskDescription);

          if (null == instanceData && null != instanceType) {
            Resource resource =
                Resource.builder().cpuUnits(ecsTaskResource.getCpu()).memoryMb(ecsTaskResource.getMemory()).build();

            Map<String, String> metaData = new HashMap<>();
            if (InstanceType.ECS_TASK_EC2 == instanceType) {
              String containerInstanceId = getIdFromArn(ecsTaskDescription.getContainerInstanceArn());
              InstanceData containerInstantData = fetchInstanceData(accountId, containerInstanceId);
              metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY,
                  containerInstantData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY));
              metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY,
                  containerInstantData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_CATEGORY));
              metaData.put(InstanceMetaDataConstants.OPERATING_SYSTEM,
                  containerInstantData.getMetaData().get(InstanceMetaDataConstants.OPERATING_SYSTEM));
              metaData.put(InstanceMetaDataConstants.CONTAINER_INSTANCE_ARN, containerInstanceId);
              metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_CPU,
                  String.valueOf(containerInstantData.getTotalResource().getCpuUnits()));
              metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY,
                  String.valueOf(containerInstantData.getTotalResource().getMemoryMb()));
              metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, containerInstanceId);
            }
            metaData.put(InstanceMetaDataConstants.TASK_ID, taskId);
            metaData.put(InstanceMetaDataConstants.REGION, ecsTaskDescription.getRegion());
            metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
            metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
            metaData.put(InstanceMetaDataConstants.LAUNCH_TYPE, ecsTaskDescription.getLaunchType());

            HarnessServiceInfo harnessServiceInfo = null;
            if (!ecsTaskDescription.getServiceName().equals("")) {
              String serviceArn = ecsTaskDescription.getServiceName();
              String serviceName = getIdFromArn(serviceArn);
              metaData.put(InstanceMetaDataConstants.ECS_SERVICE_NAME, serviceName);
              metaData.put(InstanceMetaDataConstants.ECS_SERVICE_ARN, serviceArn);
              harnessServiceInfo = getHarnessServiceInfo(accountId, clusterId, serviceName);
            }

            instanceData = InstanceData.builder()
                               .accountId(accountId)
                               .instanceId(taskId)
                               .clusterName(clusterName)
                               .clusterId(clusterId)
                               .settingId(settingId)
                               .instanceType(instanceType)
                               .instanceState(InstanceState.INITIALIZING)
                               .totalResource(resource)
                               .metaData(metaData)
                               .harnessServiceInfo(harnessServiceInfo)
                               .build();

            logger.info("Creating task {} ", taskId);
            instanceDataService.create(instanceData);
          }
        });
  }

  InstanceType getInstanceType(EcsTaskDescription ecsTaskDescription) {
    if (ecsTaskDescription.getLaunchType().equals(LaunchType.EC2.toString())) {
      return InstanceType.ECS_TASK_EC2;
    } else if (ecsTaskDescription.getLaunchType().equals(LaunchType.FARGATE.toString())) {
      return InstanceType.ECS_TASK_FARGATE;
    }
    return null;
  }

  private HarnessServiceInfo getHarnessServiceInfo(String accountId, String clusterName, String serviceName) {
    ContainerDeploymentKey containerDeploymentKey =
        ContainerDeploymentKey.builder().containerServiceName(serviceName).build();
    ContainerDeploymentInfoWithNames containerDeploymentInfoWithNames =
        ContainerDeploymentInfoWithNames.builder().clusterName(clusterName).containerSvcName(serviceName).build();
    DeploymentSummary deploymentSummary = DeploymentSummary.builder()
                                              .accountId(accountId)
                                              .containerDeploymentKey(containerDeploymentKey)
                                              .deploymentInfo(containerDeploymentInfoWithNames)
                                              .build();
    Optional<HarnessServiceInfo> harnessServiceInfo =
        cloudToHarnessMappingService.getHarnessServiceInfo(deploymentSummary);
    return harnessServiceInfo.orElse(null);
  }
}
