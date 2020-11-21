package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;

import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateAsyncTaskResponse.DelegateAsyncTaskResponseKeys;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class DelegateAsyncServiceImpl implements DelegateAsyncService {
  @Inject private HPersistence persistence;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  private static final int DELETE_TRESHOLD = 20;
  private static final long MAX_PROCESSING_DURATION_MILLIS = 60000L;

  @Override
  public void run() {
    // TODO - method guaranties at least one delivery
    Set<String> responsesToBeDeleted = new HashSet<>();

    while (true) {
      try {
        Query<DelegateAsyncTaskResponse> taskResponseQuery =
            persistence.createQuery(DelegateAsyncTaskResponse.class, excludeAuthority)
                .field(DelegateAsyncTaskResponseKeys.processAfter)
                .lessThan(currentTimeMillis() - MAX_PROCESSING_DURATION_MILLIS);

        UpdateOperations<DelegateAsyncTaskResponse> updateOperations =
            persistence.createUpdateOperations(DelegateAsyncTaskResponse.class)
                .set(DelegateAsyncTaskResponseKeys.processAfter, currentTimeMillis());

        DelegateAsyncTaskResponse lockedAsyncTaskResponse =
            persistence.findAndModify(taskResponseQuery, updateOperations, HPersistence.returnNewOptions);

        if (lockedAsyncTaskResponse == null) {
          break;
        }

        log.info("Process won the async task response {}.", lockedAsyncTaskResponse.getUuid());
        waitNotifyEngine.doneWith(lockedAsyncTaskResponse.getUuid(),
            (DelegateResponseData) kryoSerializer.asInflatedObject(lockedAsyncTaskResponse.getResponseData()));
        responsesToBeDeleted.add(lockedAsyncTaskResponse.getUuid());
        if (responsesToBeDeleted.size() >= DELETE_TRESHOLD) {
          deleteProcessedResponses(responsesToBeDeleted);
        }
      } catch (Exception ex) {
        log.info(String.format("Ignoring async task response because of the following error: %s", ex.getMessage()));
      }
    }

    deleteProcessedResponses(responsesToBeDeleted);
  }

  private boolean deleteProcessedResponses(Set<String> responsesToBeDeleted) {
    if (isEmpty(responsesToBeDeleted)) {
      return true;
    }

    boolean deleteSuccessful =
        persistence.deleteOnServer(persistence.createQuery(DelegateAsyncTaskResponse.class, excludeAuthority)
                                       .field(DelegateAsyncTaskResponseKeys.uuid)
                                       .in(responsesToBeDeleted));

    if (deleteSuccessful) {
      responsesToBeDeleted.clear();
    }

    return deleteSuccessful;
  }

  @Getter(lazy = true)
  private final byte[] timeoutMessage = kryoSerializer.asDeflatedBytes(
      ErrorNotifyResponseData.builder()
          .errorMessage("Delegate service did not provide response and the task time-outed")
          .build());

  @Override
  public void setupTimeoutForTask(String taskId, long expiry) {
    persistence.save(DelegateAsyncTaskResponse.builder()
                         .uuid(taskId)
                         .responseData(getTimeoutMessage())
                         .processAfter(expiry)
                         .build());
  }
}
