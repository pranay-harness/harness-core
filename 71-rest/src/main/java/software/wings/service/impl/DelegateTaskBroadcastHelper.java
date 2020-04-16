package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.BroadcasterFactory;
import software.wings.beans.DelegateTaskBroadcast;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AssignDelegateService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class DelegateTaskBroadcastHelper {
  public static final String STREAM_DELEGATE_PATH = "/stream/delegate/";
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;

  // "broadcastCount: nextExecutionInterval for async tasks{0:0, 1:30 Sec, 2:1 Min, 3:2 Min, 4: 4 Min, 5:8 Min,
  // afterwards : every 10 min}
  private Integer[] asyncIntervals = new Integer[] {0, 30, 60, 120, 240, 480, 900};
  // "broadcastCount: nextExecutionInterval for async tasks{0:0, 1:5 Sec, 2:1 Min, 3:2 Min, 4: 4 Min, every 5 Min
  // afterwards}
  private Integer[] syncIntervals = new Integer[] {0, 5, 60, 120, 240, 300};

  public void broadcastNewDelegateTaskAsync(DelegateTask task) {
    executorService.submit(() -> {
      try {
        rebroadcastDelegateTask(task);
      } catch (Exception e) {
        logger.error("Failed to broadcast task {} for account {}", task.getUuid(), task.getAccountId(), e);
      }
    });
  }

  public void rebroadcastDelegateTask(DelegateTask delegateTask) {
    if (delegateTask == null) {
      return;
    }
    broadcasterFactory.lookup(STREAM_DELEGATE_PATH + delegateTask.getAccountId(), true)
        .broadcast(DelegateTaskBroadcast.builder()
                       .version(delegateTask.getVersion())
                       .accountId(delegateTask.getAccountId())
                       .taskId(delegateTask.getUuid())
                       .async(delegateTask.getData().isAsync())
                       .preAssignedDelegateId(delegateTask.getPreAssignedDelegateId())
                       .alreadyTriedDelegates(delegateTask.getAlreadyTriedDelegates())
                       .build());
  }

  public long findNextBroadcastTimeForTask(DelegateTask delegateTask) {
    int delta;
    int nextBroadcastCount = delegateTask.getBroadcastCount() + 1;

    if (delegateTask.getData().isAsync()) {
      // 6 attempt onwards, its every 10 mins
      if (nextBroadcastCount < asyncIntervals.length) {
        delta = asyncIntervals[nextBroadcastCount];
      } else {
        delta = asyncIntervals[asyncIntervals.length - 1];
      }
    } else {
      // 5 attempt onwards, its every 5 mins
      if (nextBroadcastCount < syncIntervals.length) {
        delta = syncIntervals[nextBroadcastCount];
      } else {
        delta = syncIntervals[syncIntervals.length - 1];
      }
    }

    return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delta);
  }
}
