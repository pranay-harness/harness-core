package software.wings.service.impl.security;

import software.wings.api.KmsTransitionEvent;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.VaultService;

import javax.inject.Inject;

/**
 * Created by rsingh on 10/6/17.
 */
public class KmsTransitionEventListener extends AbstractQueueListener<KmsTransitionEvent> {
  @Inject private KmsService kmsService;
  @Inject private VaultService vaultService;

  @Override
  protected void onMessage(KmsTransitionEvent message) throws Exception {
    switch (message.getEncryptionType()) {
      case VAULT:
        vaultService.changeVault(
            message.getAccountId(), message.getEntityId(), message.getFromKmsId(), message.getToKmsId());
        break;

      case KMS:
        kmsService.changeKms(
            message.getAccountId(), message.getEntityId(), message.getFromKmsId(), message.getToKmsId());
        break;

      default:
        throw new IllegalArgumentException("invalid type " + message.getEncryptionType());
    }
  }
}
