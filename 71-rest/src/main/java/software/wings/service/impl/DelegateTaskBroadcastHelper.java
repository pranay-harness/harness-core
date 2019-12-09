package software.wings.service.impl;

import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AssignDelegateService;

import java.util.List;
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

  /**
   * This method will first update task with preAssignedDelegate if it finds one,
   * and then broadcast the task.
   */
  public void broadcastNewDelegateTaskAsync(final DelegateTask task) {
    executorService.submit(() -> {
      try {
        broadcastNewDelegateTask(task);
      } catch (Exception e) {
        logger.error("Failed to broadcast task {} for account {}", task.getUuid(), task.getAccountId(), e);
      }
    });
  }

  public DelegateTask broadcastNewDelegateTask(DelegateTask task) {
    String preAssignedDelegateId = assignDelegateService.pickFirstAttemptDelegate(task);

    // Update fields for DelegateTask, preAssignedDelegateId and executionCapabilities if not empty
    task = updateDelegateTaskWithPreAssignedDelegateId(task, preAssignedDelegateId, task.getExecutionCapabilities());
    rebroadcastDelegateTask(task);
    return task;
  }

  private DelegateTask updateDelegateTaskWithPreAssignedDelegateId(
      DelegateTask delegateTask, String preAssignedDelegateId, List<ExecutionCapability> executionCapabilities) {
    if (isBlank(preAssignedDelegateId) && isEmpty(executionCapabilities)) {
      // Reason here we are fetching delegateTask again from DB  is,
      // Before this call is made, we try to generate Capabilities required for this delegate Task.
      // During this, we try to evaluateExpressions, which will replace secrets in parameters with expressions,
      // like secretFunctor.obtain(),
      // We want to broadcast original DelegateTask and not this modified one.
      // So here we fetch original task and return it, so it will be broadcasted.
      return wingsPersistence.get(DelegateTask.class, delegateTask.getUuid());
    }

    Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                    .filter(DelegateTaskKeys.status, QUEUED)
                                    .field(DelegateTaskKeys.delegateId)
                                    .doesNotExist()
                                    .filter(ID_KEY, delegateTask.getUuid());

    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class);

    if (isNotBlank(preAssignedDelegateId)) {
      updateOperations.set(DelegateTaskKeys.preAssignedDelegateId, preAssignedDelegateId);
    }

    if (isNotEmpty(executionCapabilities)) {
      updateOperations.set(DelegateTaskKeys.executionCapabilities, executionCapabilities);
    }

    return wingsPersistence.findAndModifySystemData(query, updateOperations, HPersistence.returnNewOptions);
  }

  public void rebroadcastDelegateTask(DelegateTask delegateTask) {
    if (delegateTask != null) {
      broadcasterFactory.lookup(STREAM_DELEGATE_PATH + delegateTask.getAccountId(), true).broadcast(delegateTask);
    }
  }

  public long findNextBroadcastTimeForTask(DelegateTask delegateTask) {
    int delta;
    int nextBroadcastCount = delegateTask.getBroadcastCount() + 1;

    if (delegateTask.isAsync()) {
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
