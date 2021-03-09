package io.harness.accesscontrol.commons.events;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;

import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventListener implements Runnable {
  private final Consumer redisConsumer;
  private final Set<EventConsumer> eventConsumers;

  @Inject
  public EventListener(@Named(ENTITY_CRUD) Consumer redisConsumer, Set<EventConsumer> eventConsumers) {
    this.redisConsumer = redisConsumer;
    this.eventConsumers = eventConsumers;
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(ACCESS_CONTROL_SERVICE.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("Entity crud stream consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetContext();
  }

  private void pollAndProcessMessages() throws ConsumerShutdownException {
    List<Message> messages;
    String messageId;
    boolean messageProcessed;
    messages = redisConsumer.read(Duration.ofSeconds(10));
    for (Message message : messages) {
      messageId = message.getId();
      messageProcessed = handleMessage(message);
      if (messageProcessed) {
        redisConsumer.acknowledge(messageId);
      }
    }
  }

  private boolean handleMessage(Message message) {
    try {
      return processMessage(message);
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }

  private boolean processMessage(Message message) {
    if (message.getMessage() == null) {
      return true;
    }
    Set<EventConsumer> filteredConsumers =
        eventConsumers.stream()
            .filter(eventConsumer -> {
              try {
                return eventConsumer.getEventFilter().filter(message);
              } catch (Exception e) {
                log.error("Event Filter failed to filter the message {} due to exception", message, e);
                return false;
              }
            })
            .collect(Collectors.toSet());
    return filteredConsumers.stream().allMatch(eventConsumer -> {
      try {
        return eventConsumer.getEventHandler().handle(message);
      } catch (Exception e) {
        log.error("Event Handler failed to process the message {} due to exception", message, e);
        return false;
      }
    });
  }
}
