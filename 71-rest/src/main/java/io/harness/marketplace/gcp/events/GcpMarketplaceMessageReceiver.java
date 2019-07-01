package io.harness.marketplace.gcp.events;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.govern.Switch;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class GcpMarketplaceMessageReceiver implements MessageReceiver {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private final CreateAccountEventHandler createAccountEventHandler;

  public GcpMarketplaceMessageReceiver(CreateAccountEventHandler createAccountEventHandler) {
    this.createAccountEventHandler = createAccountEventHandler;
  }

  @Override
  public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
    // handle incoming message, then ack/nack the received message
    try {
      handleMessage(message);
      consumer.ack();
    } catch (IOException e) {
      logger.error("IOException processing GCP marketplace message: {}", message, e);
    } catch (Exception e) {
      logger.error("Could not process GCP marketplace message: {}", message, e);
    }
  }

  private void handleMessage(PubsubMessage pubsubMessage) throws IOException {
    String messageData = pubsubMessage.getData().toStringUtf8();
    logger.info("Got GCP marketplace message. ID: {}, Data: {}", pubsubMessage.getMessageId(), messageData);
    BaseEvent baseMessage = JSON_MAPPER.readValue(messageData, BaseEvent.class);

    EventType eventType = baseMessage.getEventType();
    switch (eventType) {
      case ACCOUNT_ACTIVE:
        AccountActiveEvent event = JSON_MAPPER.readValue(messageData, AccountActiveEvent.class);
        createAccountEventHandler.handle(pubsubMessage.getMessageId(), event);
        break;
      case ENTITLEMENT_CREATION_REQUESTED:
        Switch.noop();
        break;
      case ENTITLEMENT_ACTIVE:
        Switch.noop();
        break;
      default:
        logger.error("No handler for eventType: {}", eventType);
    }
  }
}
