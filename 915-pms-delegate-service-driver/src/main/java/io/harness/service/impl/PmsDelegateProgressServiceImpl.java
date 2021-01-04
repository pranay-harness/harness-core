package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.DelegateTaskProgressResponse.DelegateTaskProgressResponseKeys;

import static java.lang.System.currentTimeMillis;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.service.intfc.DelegateProgressService;
import io.harness.tasks.BinaryResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.client.result.DeleteResult;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
public class PmsDelegateProgressServiceImpl implements DelegateProgressService {
  @Inject private MongoTemplate persistence;
  @Inject @Named("morphiaClasses") private Map<Class, String> morphiaCustomCollectionNames;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  private static final int DELETE_TRESHOLD = 1000;
  private static final long MAX_PROCESSING_DURATION_MILLIS = 60000L;

  @Override
  public void run() {
    Set<String> responsesToBeDeleted = new HashSet<>();

    while (true) {
      try {
        Query query = query(where(DelegateTaskProgressResponseKeys.processAfter)
                                .lt(currentTimeMillis() - MAX_PROCESSING_DURATION_MILLIS));
        Update updateOperations = new Update().set(DelegateTaskProgressResponseKeys.processAfter, currentTimeMillis());
        DelegateTaskProgressResponse lockedTaskProgressResponse = persistence.findAndModify(query, updateOperations,
            DelegateTaskProgressResponse.class, morphiaCustomCollectionNames.get(DelegateTaskProgressResponse.class));

        if (lockedTaskProgressResponse == null) {
          break;
        }

        log.info("Process won the task progress response {}.", lockedTaskProgressResponse.getUuid());
        BinaryResponseData binaryResponseData =
            BinaryResponseData.builder().data(lockedTaskProgressResponse.getProgressData()).build();
        waitNotifyEngine.progressOn(lockedTaskProgressResponse.getCorrelationId(), binaryResponseData);

        responsesToBeDeleted.add(lockedTaskProgressResponse.getUuid());
        if (responsesToBeDeleted.size() >= DELETE_TRESHOLD) {
          deleteProcessedResponses(responsesToBeDeleted);
        }
      } catch (Exception e) {
        log.error("Exception occurred why processing the progress response", e);
      }
    }

    deleteProcessedResponses(responsesToBeDeleted);
  }

  private boolean deleteProcessedResponses(Set<String> responsesToBeDeleted) {
    if (isEmpty(responsesToBeDeleted)) {
      return true;
    }
    boolean deleteSuccessful = false;
    Query query = query(where(DelegateTaskProgressResponseKeys.uuid).in(responsesToBeDeleted));
    DeleteResult deleteResult =
        persistence.remove(query, morphiaCustomCollectionNames.get(DelegateTaskProgressResponse.class));

    if (deleteResult.wasAcknowledged() && deleteResult.getDeletedCount() > 0) {
      responsesToBeDeleted.clear();
      deleteSuccessful = true;
    }
    return deleteSuccessful;
  }
}
