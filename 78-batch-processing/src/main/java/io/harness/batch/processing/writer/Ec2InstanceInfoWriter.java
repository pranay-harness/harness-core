package io.harness.batch.processing.writer;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.EcsCCMConstants;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Ec2InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Qualifier("ec2InstanceInfoWriter")
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
          String clusterArn = ""; // ec2InstanceInfo.getClusterArn();
          String instanceId = ec2InstanceInfo.getInstanceId();

          boolean activeInstance = createActiveInstance(accountId, instanceId, clusterArn);
          if (activeInstance) {
            InstanceData instanceData = fetchActiveInstanceData(accountId, instanceId);
            if (null == instanceData) {
              String instanceFamily = ec2InstanceInfo.getInstanceType();
              Map<String, String> metaData = new HashMap<>();
              metaData.put(EcsCCMConstants.INSTANCE_FAMILY, instanceFamily);
              instanceData = InstanceData.builder()
                                 .accountId(accountId)
                                 .instanceId(instanceId)
                                 .clusterName(clusterArn)
                                 .instanceType(InstanceType.EC2_INSTANCE)
                                 .instanceState(InstanceState.INITIALIZING)
                                 .metaData(metaData)
                                 .build();
              logger.info("Creating ec2 instance {} ", instanceId);
              instanceDataService.create(instanceData);
            }
          }
        });
  }
}
