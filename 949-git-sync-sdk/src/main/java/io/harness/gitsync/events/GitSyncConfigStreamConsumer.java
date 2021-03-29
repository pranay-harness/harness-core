package io.harness.gitsync.events;

import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CONFIG_STREAM;

import io.harness.AuthorizationServiceHeader;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitSyncConfigStreamConsumer implements Runnable {
  private final Consumer redisConsumer;
  private final List<MessageListener> messageListenersList;
  AuthorizationServiceHeader authorizationServiceHeader;

  @Inject
  public GitSyncConfigStreamConsumer(@Named(GIT_CONFIG_STREAM) Consumer redisConsumer,
      @Named(GIT_CONFIG_STREAM) MessageListener gitSyncConfigStreamCConsumer,
      @Named("git-msvc") AuthorizationServiceHeader authorizationServiceHeader) {
    this.redisConsumer = redisConsumer;
    messageListenersList = new ArrayList<>();
    messageListenersList.add(gitSyncConfigStreamCConsumer);
    this.authorizationServiceHeader = authorizationServiceHeader;
  }

  @Override
  public void run() {
    log.info("Started the consumer for git sync config stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(authorizationServiceHeader.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("git sync config stream consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
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
    AtomicBoolean success = new AtomicBoolean(true);
    messageListenersList.forEach(messageListener -> {
      if (!messageListener.handleMessage(message)) {
        success.set(false);
      }
    });

    return success.get();
  }
}
