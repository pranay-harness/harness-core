package io.harness.batch.processing.writer;

import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Ec2Lifecycle;
import io.harness.event.payloads.Lifecycle;

import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

@Slf4j
@Singleton
public class Ec2InstanceLifecycleWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    log.info("Published batch size is Ec2InstanceLifecycleWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.EC2_INSTANCE_LIFECYCLE))
        .forEach(publishedMessage -> {
          Ec2Lifecycle ec2Lifecycle = (Ec2Lifecycle) publishedMessage.getMessage();
          log.debug("Ec2 lifecycle {} ", ec2Lifecycle);
          String accountId = publishedMessage.getAccountId();
          Lifecycle lifecycle = ec2Lifecycle.getLifecycle();
          handleLifecycleEvent(accountId, lifecycle);
        });
  }
}
