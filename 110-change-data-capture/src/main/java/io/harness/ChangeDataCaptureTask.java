package io.harness;

import io.harness.changestreamsframework.ChangeSubscriber;
import io.harness.persistence.PersistentEntity;

import software.wings.beans.Application;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeDataCaptureTask {
  @Inject private ChangeDataCaptureHelper changeDataCaptureHelper;
  @Inject private ChangeEventProcessor changeEventProcessor;

  private ChangeSubscriber<?> getChangeSubscriber() {
    return changeEvent -> {
      boolean isRunningSuccessfully = changeEventProcessor.processChangeEvent(changeEvent);
      if (!isRunningSuccessfully) {
        stop();
      }
    };
  }

  public boolean run() throws InterruptedException {
    // Register all SubscribedClasses for CDC
    Set<Class<? extends PersistentEntity>> subscribedClasses = new HashSet<>();
    subscribedClasses.add(Application.class);

    log.info("Initializing change listeners for CDC entities");
    changeDataCaptureHelper.startChangeListeners(getChangeSubscriber(), subscribedClasses);
    changeEventProcessor.startProcessingChangeEvents(subscribedClasses);
    boolean isAlive = true;
    while (!Thread.currentThread().isInterrupted() && isAlive) {
      Thread.sleep(2000);
      isAlive = changeDataCaptureHelper.checkIfAnyChangeListenerIsAlive();
      isAlive = isAlive && changeEventProcessor.isAlive();
    }
    return false;
  }

  public void stop() {
    changeDataCaptureHelper.stopChangeListeners();
    changeEventProcessor.shutdown();
  }
}