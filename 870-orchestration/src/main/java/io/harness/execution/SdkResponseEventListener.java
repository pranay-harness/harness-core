package io.harness.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.utils.SdkResponseListenerHelper;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListenerWithObservers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class SdkResponseEventListener extends QueueListenerWithObservers<SdkResponseEvent> {
  @Inject private SdkResponseListenerHelper sdkResponseListenerHelper;

  @Inject
  public SdkResponseEventListener(QueueConsumer<SdkResponseEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessageInternal(SdkResponseEvent event) {
    sdkResponseListenerHelper.handleEvent(event);
  }
}
