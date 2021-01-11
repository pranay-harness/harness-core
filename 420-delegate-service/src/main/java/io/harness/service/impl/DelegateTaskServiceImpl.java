package io.harness.service.impl;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.lang.System.currentTimeMillis;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskLogContext;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.DelegateDriverLogContext;
import io.harness.observer.Subject;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoSerializer;
import io.harness.service.dto.RetryDelegate;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.service.intfc.DelegateCallbackService;
import io.harness.service.intfc.DelegateTaskRetryObserver;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.TaskType;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.Query;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateTaskServiceImpl implements DelegateTaskService {
  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateCallbackRegistry delegateCallbackRegistry;
  @Inject private KryoSerializer kryoSerializer;

  @Getter private Subject<DelegateTaskRetryObserver> retryObserverSubject = new Subject<>();

  @Override
  public void touchExecutingTasks(String accountId, String delegateId, List<String> delegateTaskIds) {
    // Touch currently executing tasks.
    if (EmptyPredicate.isEmpty(delegateTaskIds)) {
      return;
    }

    log.info("Updating tasks");

    Query<DelegateTask> delegateTaskQuery = persistence.createQuery(DelegateTask.class)
                                                .filter(DelegateTaskKeys.accountId, accountId)
                                                .field(DelegateTaskKeys.uuid)
                                                .in(delegateTaskIds)
                                                .filter(DelegateTaskKeys.delegateId, delegateId)
                                                .filter(DelegateTaskKeys.status, DelegateTask.Status.STARTED)
                                                .project(DelegateTaskKeys.uuid, true)
                                                .project(DelegateTaskKeys.data_timeout, true);

    // TODO: it seems like mongo 4.2 supports update based on another field. Change this when we fully migrate to it.
    long now = currentTimeMillis();
    try (HIterator<DelegateTask> iterator = new HIterator<>(delegateTaskQuery.fetch())) {
      for (DelegateTask delegateTask : iterator) {
        persistence.update(delegateTask,
            persistence.createUpdateOperations(DelegateTask.class)
                .set(DelegateTaskKeys.expiry, now + delegateTask.getData().getTimeout()));
      }
    }
  }

  @Override
  public void processDelegateResponse(
      String accountId, String delegateId, String taskId, DelegateTaskResponse response) {
    if (response == null) {
      throw new InvalidArgumentsException(Pair.of("args", "response cannot be null"));
    }

    log.info("Response received for task with responseCode [{}]", response.getResponseCode());

    Query<DelegateTask> taskQuery = persistence.createQuery(DelegateTask.class)
                                        .filter(DelegateTaskKeys.accountId, response.getAccountId())
                                        .filter(DelegateTaskKeys.uuid, taskId);

    DelegateTask delegateTask = taskQuery.get();

    if (delegateTask != null) {
      try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
               TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
        if (!StringUtils.equals(delegateTask.getVersion(), getVersion())) {
          log.warn("Version mismatch for task. [managerVersion {}, taskVersion {}]", getVersion(),
              delegateTask.getVersion());
        }

        if (response.getResponseCode() == DelegateTaskResponse.ResponseCode.RETRY_ON_OTHER_DELEGATE) {
          RetryDelegate retryDelegate =
              RetryDelegate.builder().delegateId(delegateId).delegateTask(delegateTask).taskQuery(taskQuery).build();

          RetryDelegate delegateTaskRetry =
              retryObserverSubject.fireProcess(DelegateTaskRetryObxxxxxxxx:onPossibleRetry, retryDelegate);

          if (delegateTaskRetry.isRetryPossible()) {
            return;
          }
        }
        handleResponse(delegateTask, taskQuery, response);
        retryObserverSubject.fireInform(DelegateTaskRetryObxxxxxxxx:onTaskResponseProcessed, delegateTask, delegateId);
      }
    } else {
      log.warn("No delegate task found");
    }
  }

  private String getVersion() {
    return versionInfoManager.getVersionInfo().getVersion();
  }

  @Override
  public void handleResponse(DelegateTask delegateTask, Query<DelegateTask> taskQuery, DelegateTaskResponse response) {
    if (delegateTask.getDriverId() == null) {
      handleInprocResponse(delegateTask, response);
    } else {
      handleDriverResponse(delegateTask, response);
    }

    if (taskQuery != null) {
      persistence.deleteOnServer(taskQuery);
    }
  }

  @VisibleForTesting
  void handleDriverResponse(DelegateTask delegateTask, DelegateTaskResponse response) {
    if (delegateTask == null || response == null) {
      return;
    }

    DelegateCallbackService delegateCallbackService =
        delegateCallbackRegistry.obtainDelegateCallbackService(delegateTask.getDriverId());
    if (delegateCallbackService == null) {
      return;
    }

    try (DelegateDriverLogContext driverLogContext =
             new DelegateDriverLogContext(delegateTask.getDriverId(), OVERRIDE_ERROR);
         TaskLogContext taskLogContext = new TaskLogContext(delegateTask.getUuid(), OVERRIDE_ERROR)) {
      if (delegateTask.getData().isAsync()) {
        log.info("Publishing async task response...");
        delegateCallbackService.publishAsyncTaskResponse(
            delegateTask.getUuid(), kryoSerializer.asDeflatedBytes(response.getResponse()));
      } else {
        log.info("Publishing sync task response...");
        delegateCallbackService.publishSyncTaskResponse(
            delegateTask.getUuid(), kryoSerializer.asDeflatedBytes(response.getResponse()));
      }
    } catch (Exception ex) {
      log.error("Failed publishing task response for task", ex);
    }
  }

  private void handleInprocResponse(DelegateTask delegateTask, DelegateTaskResponse response) {
    if (delegateTask.getData().isAsync()) {
      String waitId = delegateTask.getWaitId();
      if (waitId != null) {
        waitNotifyEngine.doneWith(waitId, response.getResponse());
      } else {
        log.error("Async task has no wait ID");
      }
    } else {
      persistence.save(DelegateSyncTaskResponse.builder()
                           .uuid(delegateTask.getUuid())
                           .responseData(kryoSerializer.asDeflatedBytes(response.getResponse()))
                           .build());
    }
  }
}
