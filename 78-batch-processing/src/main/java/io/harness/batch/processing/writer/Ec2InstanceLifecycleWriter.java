package io.harness.batch.processing.writer;

import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Ec2Lifecycle;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Qualifier("ec2InstanceLifecycleWriter")
public class Ec2InstanceLifecycleWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    logger.info("Published batch size is Ec2InstanceLifecycleWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.EC2_INSTANCE_LIFECYCLE))
        .forEach(publishedMessage -> {
          publishedMessage.postLoad();
          Ec2Lifecycle ec2Lifecycle = (Ec2Lifecycle) publishedMessage.getMessage();
          logger.debug("Ec2 lifecycle {} ", ec2Lifecycle);
          String accountId = publishedMessage.getAccountId();
          Lifecycle lifecycle = ec2Lifecycle.getLifecycle();
          String instanceId = lifecycle.getInstanceId();

          boolean updateInstanceLifecycle = true;
          if (lifecycle.getType().equals(EventType.STOP)) {
            updateInstanceLifecycle = deleteActiveInstance(accountId, instanceId);
          }

          if (updateInstanceLifecycle) {
            logger.info("Updating instance lifecycle {} ", instanceId);
            updateInstanceDataLifecycle(accountId, instanceId, lifecycle);
          }
        });
  }
}
