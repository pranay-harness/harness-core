package io.harness.delegate.eventstream;

import static io.harness.AuthorizationServiceHeader.MANAGER;
import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PROJECT_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DEL)
@Slf4j
@Singleton
public class EntityCRUDConsumer implements Runnable {
  private static final int WAIT_TIME_IN_SECONDS = 10;
  private final Consumer redisConsumer;
  private final List<MessageListener> messageListeners;

  @Inject
  public EntityCRUDConsumer(@Named(ENTITY_CRUD) Consumer redisConsumer,
      @Named(ORGANIZATION_ENTITY + ENTITY_CRUD) MessageListener organizationEntityCRUDStreamListener,
      @Named(PROJECT_ENTITY + ENTITY_CRUD) MessageListener projectEntityCRUDStreamListener) {
    this.redisConsumer = redisConsumer;
    this.messageListeners = Lists.newArrayList(organizationEntityCRUDStreamListener, projectEntityCRUDStreamListener);
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud");
    SecurityContextBuilder.setContext(new ServicePrincipal(MANAGER.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (final Exception ex) {
      log.error("Entity crud consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
  }

  private void pollAndProcessMessages() throws InterruptedException {
    try {
      for (final Message message : redisConsumer.read(Duration.ofSeconds(WAIT_TIME_IN_SECONDS))) {
        final String messageId = message.getId();
        if (handleMessage(message)) {
          redisConsumer.acknowledge(messageId);
        }
      }
    } catch (final EventsFrameworkDownException e) {
      log.error("Events framework is down for Entity crud consumer. Retrying again...", e);
      TimeUnit.SECONDS.sleep(WAIT_TIME_IN_SECONDS);
    }
  }

  private boolean handleMessage(final Message message) {
    try {
      return processMessage(message);
    } catch (final Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }

  private boolean processMessage(final Message message) {
    final AtomicBoolean success = new AtomicBoolean(true);
    messageListeners.forEach(messageListener -> {
      if (!messageListener.handleMessage(message)) {
        success.set(false);
      }
    });
    return success.get();
  }
}
