package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse.DelegateSyncTaskResponseKeys;
import io.harness.exception.InvalidArgumentsException;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.tasks.ResponseData;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Slf4j
public class DelegateSyncServiceImpl implements DelegateSyncService {
  @Inject private HPersistence persistence;
  @Inject private KryoSerializer kryoSerializer;

  @VisibleForTesting public final ConcurrentMap<String, AtomicLong> syncTaskWaitMap = new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings({"PMD", "SynchronizationOnLocalVariableOrMethodParameter"})
  public void run() {
    try {
      if (isNotEmpty(syncTaskWaitMap)) {
        List<String> completedSyncTasks = persistence.createQuery(DelegateSyncTaskResponse.class, excludeAuthority)
                                              .field(DelegateSyncTaskResponseKeys.uuid)
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
      log.warn("Exception is of type Exception. Ignoring.", exception);
    }
  }

  @Override
  public <T extends ResponseData> T waitForTask(String taskId, String description, Duration timeout) {
    DelegateSyncTaskResponse taskResponse;
    try {
      log.info("Executing sync task");
      AtomicLong endAt =
          syncTaskWaitMap.computeIfAbsent(taskId, k -> new AtomicLong(currentTimeMillis() + timeout.toMillis()));
      synchronized (endAt) {
        while (currentTimeMillis() < endAt.get()) {
          endAt.wait(timeout.toMillis());
        }
      }
      taskResponse = persistence.get(DelegateSyncTaskResponse.class, taskId);
    } catch (Exception e) {
      throw new InvalidArgumentsException(Pair.of("args", "Error while waiting for completion"), e);
    } finally {
      syncTaskWaitMap.remove(taskId);
      persistence.delete(DelegateSyncTaskResponse.class, taskId);
    }

    if (taskResponse == null) {
      throw new InvalidArgumentsException(Pair.of("args",
          "Task has expired. It wasn't picked up by any delegate or delegate did not have enough time to finish the execution."));
    }

    return (T) kryoSerializer.asInflatedObject(taskResponse.getResponseData());
  }
}
