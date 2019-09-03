package io.harness.batch.processing.writer;

import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsTaskLifecycle;
import io.harness.event.payloads.Lifecycle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Qualifier("ecsTaskLifecycleWriter")
public class EcsTaskLifecycleWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    logger.info("Published batch size is EcsTaskLifecycleWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.ECS_TASK_LIFECYCLE))
        .forEach(publishedMessage -> {
          EcsTaskLifecycle ecsTaskLifecycle = (EcsTaskLifecycle) publishedMessage.getMessage();
          logger.debug("ECS task lifecycle {} ", ecsTaskLifecycle);
          String accountId = publishedMessage.getAccountId();
          Lifecycle lifecycle = ecsTaskLifecycle.getLifecycle();
          handleLifecycleEvent(accountId, lifecycle);
        });
  }
}
