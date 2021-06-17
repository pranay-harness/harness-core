package io.harness.ng.authenticationsettings;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkConstants.SAML_AUTHORIZATION_ASSERTION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.samlauthorization.samlauthorizationdata.SamlAuthorizationDTO;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class SamlAuthorizationStreamConsumer implements Runnable {
  private final Consumer redisConsumer;
  @Inject private NGSamlUserGroupSync ngSamlUserGroupSync;
  @Inject
  public SamlAuthorizationStreamConsumer(@Named(SAML_AUTHORIZATION_ASSERTION) Consumer redisConsumer) {
    this.redisConsumer = redisConsumer;
  }

  @Override
  public void run() {
    log.info("Started the consumer for saml assertion stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(NG_MANAGER.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("saml assertion stream consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
  }

  private void pollAndProcessMessages() {
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
      processMessage(message);
      return true;
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }

  private void processMessage(Message message) {
    if (message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      SamlAuthorizationDTO samlAuthorizationDTO;
      try {
        samlAuthorizationDTO = SamlAuthorizationDTO.parseFrom(message.getMessage().getData());
        ngSamlUserGroupSync.syncUserGroup(samlAuthorizationDTO.getAccountIdentifier(), samlAuthorizationDTO.getSsoId(),
            samlAuthorizationDTO.getEmail(), samlAuthorizationDTO.getUserGroupsList());
      } catch (InvalidProtocolBufferException e) {
        log.error("Exception in unpacking samlAuthorizationDTO for key {}", message.getId(), e);
        throw new IllegalStateException(e);
      }
    }
  }
}
