package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.KmsTransitionEvent;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;

/**
 * Created by rsingh on 10/6/17.
 */
@OwnedBy(PL)
@Slf4j
public class KmsTransitionEventListener extends QueueListener<KmsTransitionEvent> {
  @Inject private SecretManager secretManager;

  @Inject
  public KmsTransitionEventListener(QueueConsumer<KmsTransitionEvent> queueConsumer) {
    super(queueConsumer, true);
  }

  @Override
  public void onMessage(KmsTransitionEvent message) {
    log.info("Processing secret manager transition event for secret '{}' in account '{}'", message.getEntityId(),
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
        log.warn("Transitioning secret '{}' failed. trial num: {}", message, failedAttempts);
        if (failedAttempts == NUM_OF_RETRIES) {
          log.error("Transitioning secret '{}' failed after {} retries", message, NUM_OF_RETRIES, e);
          break;
        }
        sleep(ofMillis(1000));
      } catch (IllegalStateException | IOException e) {
        log.error("Could not transition secret '{}'", message, e);
        break;
      }
    }
  }
}
