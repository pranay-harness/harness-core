package io.harness.pms.notification.orchestration.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.PipelineEventType;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.notification.NotificationHelper;
import io.harness.pms.notification.orchestration.observers.NotificationObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NotificationInformHandler implements AsyncInformObserver, NotificationObserver {
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;
  @Inject NotificationHelper notificationHelper;

  @Override
  public void onSuccess(Ambiance ambiance) {
    notificationHelper.sendNotification(ambiance, PipelineEventType.PIPELINE_SUCCESS, null);
  }

  @Override
  public void onPause(Ambiance ambiance) {
    notificationHelper.sendNotification(ambiance, PipelineEventType.PIPELINE_PAUSED, null);
  }

  @Override
  public void onFailure(Ambiance ambiance) {
    notificationHelper.sendNotification(ambiance, PipelineEventType.PIPELINE_FAILED, null);
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
