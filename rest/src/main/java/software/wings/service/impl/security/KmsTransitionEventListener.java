package software.wings.service.impl.security;

import software.wings.api.KmsTransitionEvent;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.intfc.security.KmsService;

import javax.inject.Inject;

/**
 * Created by rsingh on 10/6/17.
 */
public class KmsTransitionEventListener extends AbstractQueueListener<KmsTransitionEvent> {
  @Inject private KmsService kmsService;
  @Override
  protected void onMessage(KmsTransitionEvent message) throws Exception {
    kmsService.changeKms(message.getAccountId(), message.getEntityId(), message.getFromKmsId(), message.getToKmsId());
  }
}
