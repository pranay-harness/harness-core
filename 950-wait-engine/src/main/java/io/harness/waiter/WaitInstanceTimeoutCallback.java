package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutInstance;
import io.harness.waiter.persistence.PersistenceWrapper;

import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class WaitInstanceTimeoutCallback implements TimeoutCallback {
  @Inject @Transient Injector injector;
  @Inject @Transient PersistenceWrapper persistenceWrapper;

  String waitInstanceId;

  @Builder
  public WaitInstanceTimeoutCallback(String waitInstanceId) {
    this.waitInstanceId = waitInstanceId;
  }

  @Override
  public void onTimeout(TimeoutInstance timeoutInstance) {
    long now = System.currentTimeMillis();
    WaitInstance waitInstance = persistenceWrapper.fetchForProcessingWaitInstance(waitInstanceId, now);
    if (waitInstance == null) {
      log.warn("Wait Instance already handled");
      return;
    }
    ProcessedMessageResponse processedMessageResponse = persistenceWrapper.processMessage(waitInstance);
    NotifyCallback callback = waitInstance.getCallback();
    if (callback != null) {
      injector.injectMembers(callback);
      callback.notifyTimeout(processedMessageResponse.getResponseDataMap());
    }

    try {
      persistenceWrapper.deleteWaitInstance(waitInstance);
    } catch (Exception exception) {
      log.error("Failed to delete WaitInstance", exception);
    }
  }
}
