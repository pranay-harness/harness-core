package io.harness.ng.core.event;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkConstants.USERMEMBERSHIP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class UserMembershipStreamConsumer implements Runnable {
  private final Consumer eventConsumer;
  private final List<MessageListener> messageListenersList;

  @Inject
  public UserMembershipStreamConsumer(@Named(USERMEMBERSHIP) Consumer eventConsumer,
      @Named(USERMEMBERSHIP) MessageListener userMembershipStreamListener) {
    this.eventConsumer = eventConsumer;
    messageListenersList = new ArrayList<>();
    messageListenersList.add(userMembershipStreamListener);
  }

  @Override
  public void run() {
    log.info("Started the consumer for " + USERMEMBERSHIP + " stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(NG_MANAGER.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error(USERMEMBERSHIP + " stream consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
  }

  private void pollAndProcessMessages() {
    String messageId;
    boolean messageProcessed;
    List<Message> messages;
    messages = eventConsumer.read(Duration.ofSeconds(30));
    for (Message message : messages) {
      messageId = message.getId();

      messageProcessed = handleMessage(message);
      if (messageProcessed) {
        eventConsumer.acknowledge(messageId);
      }
    }
  }

  private boolean handleMessage(Message message) {
    try {
      AtomicBoolean success = new AtomicBoolean(true);
      messageListenersList.forEach(messageListener -> {
        if (!messageListener.handleMessage(message)) {
          success.set(false);
        }
      });
      return success.get();
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }
}
