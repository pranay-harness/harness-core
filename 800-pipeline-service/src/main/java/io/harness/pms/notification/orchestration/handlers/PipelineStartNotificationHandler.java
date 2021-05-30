package io.harness.pms.notification.orchestration.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.notification.PipelineEventType;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.notification.NotificationHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineStartNotificationHandler implements AsyncInformObserver, OrchestrationStartObserver {
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;

  @Inject NotificationHelper notificationHelper;

  @Override
  public void onStart(Ambiance ambiance) {
    notificationHelper.sendNotification(ambiance, PipelineEventType.PIPELINE_START, null);
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}