package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;

import software.wings.beans.DelegateTaskBroadcast;
import software.wings.service.intfc.AssignDelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;

@Singleton
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@OwnedBy(DEL)
public class DelegateTaskBroadcastHelper {
  public static final String STREAM_DELEGATE_PATH = "/stream/delegate/";
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private HPersistence persistence;
  @Inject private ExecutorService executorService;
  @Inject private FeatureFlagService featureFlagService;

  // "broadcastCount: nextExecutionInterval for async tasks{0:0, 1:30 Sec, 2:1 Min, 3:2 Min, 4: 4 Min, 5:8 Min,
  // afterwards : every 10 min}
  private Integer[] asyncIntervals = new Integer[] {0, 30, 60, 120, 240, 480, 900};
  // "broadcastCount: nextExecutionInterval for async tasks{0:0, 1:5 Sec, 2:1 Min, 3:2 Min, 4: 4 Min, every 5 Min
  // afterwards}
  private Integer[] syncIntervals = new Integer[] {0, 5, 60, 120, 240, 300};

  // with every broadcast interval , broadcast only max number of delegates at a time for sync/async task
  private Integer[] maxNumberOfDelegatesToBroadcast = new Integer[] {1, 2, 3, 5, 8, 10};

  public void broadcastNewDelegateTaskAsync(DelegateTask task) {
    executorService.submit(() -> {
      try {
        rebroadcastDelegateTask(task);
      } catch (Exception e) {
        log.error("Failed to broadcast task {} for account {}", task.getUuid(), task.getAccountId(), e);
      }
    });
  }

  public void rebroadcastDelegateTask(DelegateTask delegateTask) {
    if (delegateTask == null) {
      return;
    }

    DelegateTaskBroadcast delegateTaskBroadcast = DelegateTaskBroadcast.builder()
                                                      .version(delegateTask.getVersion())
                                                      .accountId(delegateTask.getAccountId())
                                                      .taskId(delegateTask.getUuid())
                                                      .async(delegateTask.getData().isAsync())
                                                      .broadcastToDelegatesIds(delegateTask.getBroadcastToDelegateIds())
                                                      .build();

    Broadcaster broadcaster = broadcasterFactory.lookup(STREAM_DELEGATE_PATH + delegateTask.getAccountId(), true);
    broadcaster.broadcast(delegateTaskBroadcast);
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

  public int getMaxBroadcastCount(DelegateTask delegateTask) {
    int nextBroadcastCount = delegateTask.getBroadcastCount() + 1;
    return (nextBroadcastCount < maxNumberOfDelegatesToBroadcast.length)
        ? maxNumberOfDelegatesToBroadcast[nextBroadcastCount]
        : maxNumberOfDelegatesToBroadcast[nextBroadcastCount - 1];
  }
}
