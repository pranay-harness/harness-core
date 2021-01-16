package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.System.currentTimeMillis;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateAsyncTaskResponse.DelegateAsyncTaskResponseKeys;
import io.harness.exception.FailureType;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.FailureResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.client.result.DeleteResult;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
public class PmsDelegateAsyncServiceImpl implements DelegateAsyncService {
  @Inject private MongoTemplate persistence;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named("morphiaClasses") Map<Class, String> morphiaCustomCollectionNames;
  private static final int DELETE_TRESHOLD = 20;
  private static final long MAX_PROCESSING_DURATION_MILLIS = 600000L;

  @Override
  public void run() {
    // TODO - method guaranties at least one delivery
    Set<String> responsesToBeDeleted = new HashSet<>();

    while (true) {
      try {
        Query query = query(
            where(DelegateAsyncTaskResponseKeys.processAfter).lt(currentTimeMillis() - MAX_PROCESSING_DURATION_MILLIS));
        Update updateOperations = new Update().set(DelegateAsyncTaskResponseKeys.processAfter, currentTimeMillis());
        DelegateAsyncTaskResponse lockedAsyncTaskResponse = persistence.findAndModify(query, updateOperations,
            DelegateAsyncTaskResponse.class, morphiaCustomCollectionNames.get(DelegateAsyncTaskResponse.class));
        if (lockedAsyncTaskResponse == null) {
          break;
        }

        log.info("Process won the async task response {}.", lockedAsyncTaskResponse.getUuid());
        waitNotifyEngine.doneWith(lockedAsyncTaskResponse.getUuid(),
            BinaryResponseData.builder().data(lockedAsyncTaskResponse.getResponseData()).build());
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
    boolean deleteSuccessful = false;
    Query query = query(where("_id").in(responsesToBeDeleted));
    DeleteResult deleteResult =
        persistence.remove(query, morphiaCustomCollectionNames.get(DelegateAsyncTaskResponse.class));

    if (deleteResult.wasAcknowledged() && deleteResult.getDeletedCount() > 0) {
      responsesToBeDeleted.clear();
      deleteSuccessful = true;
    }

    return deleteSuccessful;
  }

  @Getter(lazy = true)
  private final byte[] timeoutMessage = kryoSerializer.asDeflatedBytes(
      FailureResponseData.builder()
          .errorMessage("Delegate service did not provide response and the task time-outed")
          .failureTypes(EnumSet.of(FailureType.EXPIRED))
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
