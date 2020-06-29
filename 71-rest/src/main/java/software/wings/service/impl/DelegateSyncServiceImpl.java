package software.wings.service.impl;

import static io.harness.beans.DelegateTask.Status.ABORTED;
import static io.harness.beans.DelegateTask.Status.ERROR;
import static io.harness.beans.DelegateTask.Status.FINISHED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.TimeoutException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.service.intfc.DelegateSyncService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@Slf4j
public class DelegateSyncServiceImpl implements DelegateSyncService {
  private static final Set<DelegateTask.Status> TASK_COMPLETED_STATUSES = ImmutableSet.of(FINISHED, ABORTED, ERROR);

  @Inject private HPersistence persistence;

  final ConcurrentMap<String, AtomicLong> syncTaskWaitMap = new ConcurrentHashMap<>();

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  @SuppressWarnings({"PMD", "SynchronizationOnLocalVariableOrMethodParameter"})
  public void run() {
    try {
      if (isNotEmpty(syncTaskWaitMap)) {
        List<String> completedSyncTasks = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                              .filter(DelegateTaskKeys.data_async, false)
                                              .field(DelegateTaskKeys.status)
                                              .in(TASK_COMPLETED_STATUSES)
                                              .field(DelegateTaskKeys.uuid)
                                              .in(syncTaskWaitMap.keySet())
                                              .asKeyList()
                                              .stream()
                                              .map(key -> key.getId().toString())
                                              .collect(toList());
        for (String taskId : completedSyncTasks) {
          AtomicLong endAt = syncTaskWaitMap.get(taskId);
          if (endAt != null) {
            synchronized (endAt) {
              endAt.set(0L);
              endAt.notifyAll();
            }
          }
        }
      }
    } catch (Exception exception) {
      logger.warn("Exception is of type Exception. Ignoring.", exception);
    }
  }

  @Override
  public <T extends ResponseData> T waitForTask(DelegateTask task) {
    ResponseData responseData;
    DelegateTask completedTask;
    try {
      logger.info("Executing sync task");
      AtomicLong endAt = syncTaskWaitMap.computeIfAbsent(
          task.getUuid(), k -> new AtomicLong(currentTimeMillis() + task.getData().getTimeout()));
      synchronized (endAt) {
        while (currentTimeMillis() < endAt.get()) {
          endAt.wait(task.getData().getTimeout());
        }
      }
      completedTask = persistence.get(DelegateTask.class, task.getUuid());
    } catch (Exception e) {
      throw new InvalidArgumentsException(Pair.of("args", "Error while waiting for completion"));
    } finally {
      syncTaskWaitMap.remove(task.getUuid());
      persistence.delete(persistence.createQuery(DelegateTask.class)
                             .filter(DelegateTaskKeys.accountId, task.getAccountId())
                             .filter(DelegateTaskKeys.uuid, task.getUuid()));
    }

    if (completedTask == null) {
      logger.info("Task was deleted while waiting for completion");
      throw new InvalidArgumentsException(Pair.of("args", "Task was deleted while waiting for completion"));
    }

    responseData = completedTask.getNotifyResponse();
    if (responseData == null || !TASK_COMPLETED_STATUSES.contains(completedTask.getStatus())) {
      throw new TimeoutException("Delegate task", task.calcDescription(), USER_ADMIN);
    }
    logger.info("Returning response to calling function for delegate task");

    return (T) responseData;
  }
}
