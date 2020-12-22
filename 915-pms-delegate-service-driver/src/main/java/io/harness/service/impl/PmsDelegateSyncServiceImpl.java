package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.System.currentTimeMillis;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse.DelegateSyncTaskResponseKeys;
import io.harness.exception.InvalidArgumentsException;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.tasks.BinaryResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@Singleton
@Slf4j
public class PmsDelegateSyncServiceImpl implements DelegateSyncService {
  @Inject private MongoTemplate persistence;
  @Inject @Named("morphiaClasses") Map<Class, String> morphiaCustomCollectionNames;

  public final ConcurrentMap<String, AtomicLong> syncTaskWaitMap = new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings({"PMD", "SynchronizationOnLocalVariableOrMethodParameter"})
  public void run() {
    try {
      if (isNotEmpty(syncTaskWaitMap)) {
        Query query = query(where(DelegateSyncTaskResponseKeys.uuid).in(syncTaskWaitMap.keySet()));
        query.fields().include(DelegateSyncTaskResponseKeys.uuid);
        List<String> completedSyncTasks =
            persistence.find(query, String.class, morphiaCustomCollectionNames.get(DelegateSyncTaskResponse.class));
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
  public BinaryResponseData waitForTask(String taskId, String description, Duration timeout) {
    DelegateSyncTaskResponse taskResponse;
    Query query = query(where(DelegateSyncTaskResponseKeys.uuid).is(taskId));
    String collectionName = morphiaCustomCollectionNames.get(DelegateSyncTaskResponse.class);
    try {
      log.info("Executing sync task");
      AtomicLong endAt =
          syncTaskWaitMap.computeIfAbsent(taskId, k -> new AtomicLong(currentTimeMillis() + timeout.toMillis()));
      synchronized (endAt) {
        while (currentTimeMillis() < endAt.get()) {
          endAt.wait(timeout.toMillis());
        }
      }
      taskResponse = persistence.findOne(query, DelegateSyncTaskResponse.class, collectionName);
    } catch (Exception e) {
      throw new InvalidArgumentsException(Pair.of("args", "Error while waiting for completion"), e);
    } finally {
      syncTaskWaitMap.remove(taskId);
      persistence.remove(query, DelegateSyncTaskResponse.class, collectionName);
    }

    if (taskResponse == null) {
      throw new InvalidArgumentsException(Pair.of("args",
          "Task has expired. It wasn't picked up by any delegate or delegate did not have enough time to finish the execution."));
    }

    return BinaryResponseData.builder().data(taskResponse.getResponseData()).build();
  }
}
