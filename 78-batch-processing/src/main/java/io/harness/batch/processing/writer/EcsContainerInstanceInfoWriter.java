package io.harness.batch.processing.writer;

import io.harness.batch.processing.ccm.InstanceResource;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.EcsCCMConstants;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsContainerInstanceDescription;
import io.harness.event.payloads.EcsContainerInstanceInfo;
import io.harness.event.payloads.ReservedResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Qualifier("ecsContainerInstanceInfoWriter")
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
          String containerInstanceArn = ecsContainerInstanceDescription.getContainerInstanceArn();
          String clusterArn = ecsContainerInstanceDescription.getClusterArn();
          String ec2InstanceId = ecsContainerInstanceDescription.getEc2InstanceId();

          InstanceData instanceData = fetchActiveInstanceData(accountId, containerInstanceArn);

          if (null == instanceData) {
            InstanceData ec2InstanceData = fetchInstanceData(accountId, ec2InstanceId);
            Map<String, String> metaData = new HashMap<>();
            metaData.put(
                EcsCCMConstants.INSTANCE_FAMILY, ec2InstanceData.getMetaData().get(EcsCCMConstants.INSTANCE_FAMILY));
            metaData.put(EcsCCMConstants.EC2_INSTANCE_ID, ec2InstanceId);
            metaData.put(EcsCCMConstants.OPERATING_SYSTEM, ecsContainerInstanceDescription.getOperatingSystem());
            metaData.put(EcsCCMConstants.REGION, ecsContainerInstanceDescription.getRegion());

            InstanceResource instanceResource = InstanceResource.builder()
                                                    .cpu(ecsContainerInstanceResource.getCpu())
                                                    .memory(ecsContainerInstanceResource.getMemory())
                                                    .build();

            instanceData = InstanceData.builder()
                               .accountId(accountId)
                               .instanceId(containerInstanceArn)
                               .clusterName(clusterArn)
                               .instanceType(InstanceType.ECS_CONTAINER_INSTANCE)
                               .instanceState(InstanceState.INITIALIZING)
                               .instanceResource(instanceResource)
                               .metaData(metaData)
                               .build();
            logger.info("Creating container instance {} ", containerInstanceArn);
            instanceDataService.create(instanceData);
          }
        });
  }
}
