package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse.DelegateSyncTaskResponseKeys;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ResponseData;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateSyncServiceImpl implements DelegateSyncService {
  @Inject private HPersistence persistence;
  @Inject private KryoSerializer kryoSerializer;
  @Inject @Named("disableDeserialization") private boolean disableDeserialization;

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
          log.debug("Found response for sync task {}", taskId);
          AtomicLong endAt = syncTaskWaitMap.get(taskId);
          if (endAt != null) {
            synchronized (endAt) {
              log.debug("Notifying threads for task {}", taskId);
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
      log.info("Executing sync task {}", taskId);
      AtomicLong endAt =
          syncTaskWaitMap.computeIfAbsent(taskId, k -> new AtomicLong(currentTimeMillis() + timeout.toMillis()));
      synchronized (endAt) {
        while (endAt.get() != 0 && currentTimeMillis() < endAt.get()) {
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
      throw new InvalidArgumentsException(
          "Task has expired. It wasn't picked up by any delegate or delegate did not have enough time to finish the execution.");
    }

    if (disableDeserialization) {
      return (T) BinaryResponseData.builder().data(taskResponse.getResponseData()).build();
    }
    // throw exception here
    Object response = kryoSerializer.asInflatedObject(taskResponse.getResponseData());
    if (response instanceof ErrorNotifyResponseData) {
      WingsException exception = ((ErrorNotifyResponseData) response).getException();
      // if task registered to error handling framework on delegate, then exception won't be null
      if (exception != null) {
        throw exception;
      }
    }

    log.info("Deserialize and return the response for task {}", taskId);
    return (T) kryoSerializer.asInflatedObject(taskResponse.getResponseData());
  }
}
