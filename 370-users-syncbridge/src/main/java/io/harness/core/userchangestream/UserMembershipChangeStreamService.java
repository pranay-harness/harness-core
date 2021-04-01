package io.harness.core.userchangestream;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserMembershipChangeStreamService implements Managed {
  @Inject UserMembershipChangeStreamTask userMembershipChangeStreamTask;
  final ExecutorService executorService = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("user-changestream-main-thread").build());
  final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("user-changestream-main-scheduler-thread").build());
  Future<?> userChangeStreamTaskFuture;
  Future<?> scheduleUserChangeStreamTaskFuture;

  @Override
  public void start() {
    scheduleUserChangeStreamTaskFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
      if ((!Thread.currentThread().isInterrupted())
          && (userChangeStreamTaskFuture == null || userChangeStreamTaskFuture.isCancelled()
              || userChangeStreamTaskFuture.isDone())) {
        userChangeStreamTaskFuture = executorService.submit(userMembershipChangeStreamTask);
      }
    }, 5, 60, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    scheduleUserChangeStreamTaskFuture.cancel(true);
    scheduledExecutorService.shutdownNow();
    executorService.shutdownNow();
  }
}