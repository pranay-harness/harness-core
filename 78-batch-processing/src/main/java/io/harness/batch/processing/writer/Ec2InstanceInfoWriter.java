package io.harness.batch.processing.writer;

import com.google.inject.Singleton;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Ec2InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class Ec2InstanceInfoWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    logger.info("Published batch size is Ec2InstanceInfoWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.EC2_INSTANCE_INFO))
        .forEach(publishedMessage -> {
          Ec2InstanceInfo ec2InstanceInfo = (Ec2InstanceInfo) publishedMessage.getMessage();
          logger.debug("Message {} ", ec2InstanceInfo);
          String accountId = publishedMessage.getAccountId();
          String clusterArn = ec2InstanceInfo.getClusterArn();
          String instanceId = ec2InstanceInfo.getInstanceId();

          InstanceData instanceData = fetchActiveInstanceData(accountId, instanceId);
          if (null == instanceData) {
            String instanceFamily = ec2InstanceInfo.getInstanceType();
            Map<String, String> metaData = new HashMap<>();
            metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, instanceFamily);
            metaData.put(InstanceMetaDataConstants.REGION, ec2InstanceInfo.getRegion());
            metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
            instanceData = InstanceData.builder()
                               .accountId(accountId)
                               .instanceId(instanceId)
                               .clusterName(getIdFromArn(clusterArn))
                               .instanceType(InstanceType.EC2_INSTANCE)
                               .instanceState(InstanceState.INITIALIZING)
                               .metaData(metaData)
                               .build();
            logger.info("Creating ec2 instance {} ", instanceId);
            instanceDataService.create(instanceData);
          }
        });
  }
}
