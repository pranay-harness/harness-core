package software.wings.service.impl.security;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.KmsTransitionEvent;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;

/**
 * Created by rsingh on 10/6/17.
 */
public class KmsTransitionEventListener extends AbstractQueueListener<KmsTransitionEvent> {
  protected static final Logger logger = LoggerFactory.getLogger(KmsTransitionEventListener.class);
  @Inject private SecretManager secretManager;

  public KmsTransitionEventListener() {
    super(true);
  }

  @Override
  protected void onMessage(KmsTransitionEvent message) {
    try {
      logger.info("Processing secret manager transition event with entityId: {}", message.getEntityId());
      secretManager.changeSecretManager(message.getAccountId(), message.getEntityId(), message.getFromEncryptionType(),
          message.getFromKmsId(), message.getToEncryptionType(), message.getToKmsId());
    } catch (IOException e) {
      logger.error("Could not migrate " + message, e);
    }
  }
}
