package software.wings.service.impl.security;

import software.wings.api.KmsTransitionEvent;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.intfc.security.SecretManager;

import javax.inject.Inject;

/**
 * Created by rsingh on 10/6/17.
 */
public class KmsTransitionEventListener extends AbstractQueueListener<KmsTransitionEvent> {
  @Inject private SecretManager secretManager;

  @Override
  protected void onMessage(KmsTransitionEvent message) throws Exception {
    secretManager.changeSecretManager(message.getAccountId(), message.getEntityId(), message.getFromEncryptionType(),
        message.getFromKmsId(), message.getToEncryptionType(), message.getToKmsId());
  }
}
