package software.wings.service.impl.security;

import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.queue.QueueListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.KmsTransitionEvent;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;

/**
 * Created by rsingh on 10/6/17.
 */
public class KmsTransitionEventListener extends QueueListener<KmsTransitionEvent> {
  protected static final Logger logger = LoggerFactory.getLogger(KmsTransitionEventListener.class);
  @Inject private SecretManager secretManager;

  public KmsTransitionEventListener() {
    super(true);
  }

  @Override
  public void onMessage(KmsTransitionEvent message) {
    logger.info("Processing secret manager transition event for secret '{}' in account '{}'", message.getEntityId(),
        message.getAccountId());
    int failedAttempts = 0;
    while (true) {
      try {
        secretManager.changeSecretManager(message.getAccountId(), message.getEntityId(),
            message.getFromEncryptionType(), message.getFromKmsId(), message.getToEncryptionType(),
            message.getToKmsId());
        break;
      } catch (WingsException e) {
        failedAttempts++;
        logger.warn("Transitioning secret '{}' failed. trial num: {}", message, failedAttempts);
        if (failedAttempts == NUM_OF_RETRIES) {
          logger.error("Transitioning secret '{}' failed after {} retries", message, NUM_OF_RETRIES, e);
          break;
        }
        sleep(ofMillis(1000));
      } catch (IllegalStateException | IOException e) {
        logger.error("Could not transition secret '{}'", message, e);
        break;
      }
    }
  }
}
