package io.harness.batch.processing.writer;

import com.google.inject.Singleton;

import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsContainerInstanceDescription;
import io.harness.event.payloads.EcsContainerInstanceInfo;
import io.harness.event.payloads.ReservedResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class EcsContainerInstanceInfoWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    logger.info("Published batch size is EcsContainerInstanceInfoWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.ECS_CONTAINER_INSTANCE_INFO))
        .forEach(publishedMessage -> {
          EcsContainerInstanceInfo ecsContainerInstanceInfo = (EcsContainerInstanceInfo) publishedMessage.getMessage();
          logger.debug("Message {} ", ecsContainerInstanceInfo);
          EcsContainerInstanceDescription ecsContainerInstanceDescription =
              ecsContainerInstanceInfo.getEcsContainerInstanceDescription();
          ReservedResource ecsContainerInstanceResource = ecsContainerInstanceInfo.getEcsContainerInstanceResource();
          String accountId = publishedMessage.getAccountId();
          String containerInstanceId = getIdFromArn(ecsContainerInstanceDescription.getContainerInstanceArn());
          String clusterArn = ecsContainerInstanceDescription.getClusterArn();
          String ec2InstanceId = ecsContainerInstanceDescription.getEc2InstanceId();
          String clusterId = ecsContainerInstanceDescription.getClusterId();
          String settingId = ecsContainerInstanceDescription.getSettingId();

          InstanceData instanceData = fetchActiveInstanceData(accountId, containerInstanceId);

          if (null == instanceData) {
            InstanceData ec2InstanceData = fetchInstanceData(accountId, ec2InstanceId);
            Map<String, String> metaData = new HashMap<>();
            metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY,
                ec2InstanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY));
            metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY,
                ec2InstanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_CATEGORY));
            metaData.put(InstanceMetaDataConstants.EC2_INSTANCE_ID, ec2InstanceId);
            metaData.put(
                InstanceMetaDataConstants.OPERATING_SYSTEM, ecsContainerInstanceDescription.getOperatingSystem());
            metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
            metaData.put(InstanceMetaDataConstants.REGION, ecsContainerInstanceDescription.getRegion());
            metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
            metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, ec2InstanceId);

            Resource resource = Resource.builder()
                                    .cpuUnits(ecsContainerInstanceResource.getCpu())
                                    .memoryMb(ecsContainerInstanceResource.getMemory())
                                    .build();

            instanceData = InstanceData.builder()
                               .accountId(accountId)
                               .instanceId(containerInstanceId)
                               .clusterName(getIdFromArn(clusterArn))
                               .clusterId(clusterId)
                               .settingId(settingId)
                               .instanceType(InstanceType.ECS_CONTAINER_INSTANCE)
                               .instanceState(InstanceState.INITIALIZING)
                               .totalResource(resource)
                               .metaData(metaData)
                               .build();
            logger.info("Creating container instance {} ", containerInstanceId);
            instanceDataService.create(instanceData);
          }
        });
  }
}
