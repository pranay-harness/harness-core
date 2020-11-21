package io.harness.batch.processing.writer;

import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsContainerInstanceLifecycle;
import io.harness.event.payloads.Lifecycle;

import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

@Slf4j
@Singleton
public class EcsContainerInstanceLifecycleWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    log.info("Published batch size is EcsContainerInstanceLifecycleWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(
            publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.ECS_CONTAINER_INSTANCE_LIFECYCLE))
        .forEach(publishedMessage -> {
          EcsContainerInstanceLifecycle ecsContainerInstanceLifecycle =
              (EcsContainerInstanceLifecycle) publishedMessage.getMessage();
          log.debug("ECS container instance lifecycle {} ", ecsContainerInstanceLifecycle);
          String accountId = publishedMessage.getAccountId();
          Lifecycle lifecycle = ecsContainerInstanceLifecycle.getLifecycle();
          handleLifecycleEvent(accountId, lifecycle);
        });
  }
}
