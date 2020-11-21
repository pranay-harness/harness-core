package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.DelegateTaskProgressResponse.DelegateTaskProgressResponseKeys;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateProgressService;
import io.harness.waiter.WaitNotifyEngineV2;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class DelegateProgressServiceImpl implements DelegateProgressService {
  private HPersistence persistence;
  private KryoSerializer kryoSerializer;
  private WaitNotifyEngineV2 waitNotifyEngine;
  private static final int DELETE_TRESHOLD = 1000;
  private static final long MAX_PROCESSING_DURATION_MILLIS = 60000L;

  @Inject
  public DelegateProgressServiceImpl(
      HPersistence persistence, KryoSerializer kryoSerializer, WaitNotifyEngineV2 waitNotifyEngine) {
    this.persistence = persistence;
    this.kryoSerializer = kryoSerializer;
    this.waitNotifyEngine = waitNotifyEngine;
  }

  @Override
  public void run() {
    Set<String> responsesToBeDeleted = new HashSet<>();

    while (true) {
      Query<DelegateTaskProgressResponse> taskResponseQuery =
          persistence.createQuery(DelegateTaskProgressResponse.class, excludeAuthority)
              .field(DelegateTaskProgressResponseKeys.processAfter)
              .lessThan(currentTimeMillis() - MAX_PROCESSING_DURATION_MILLIS);

      UpdateOperations<DelegateTaskProgressResponse> updateOperations =
          persistence.createUpdateOperations(DelegateTaskProgressResponse.class)
              .set(DelegateTaskProgressResponseKeys.processAfter, currentTimeMillis());

      DelegateTaskProgressResponse lockedTaskProgressResponse =
          persistence.findAndModify(taskResponseQuery, updateOperations, HPersistence.returnNewOptions);

      if (lockedTaskProgressResponse == null) {
        break;
      }

      log.info("Process won the task progress response {}.", lockedTaskProgressResponse.getUuid());

      waitNotifyEngine.progressUpdate(lockedTaskProgressResponse.getUuid(),
          (DelegateResponseData) kryoSerializer.asInflatedObject(lockedTaskProgressResponse.getResponseData()));

      responsesToBeDeleted.add(lockedTaskProgressResponse.getUuid());
      if (responsesToBeDeleted.size() >= DELETE_TRESHOLD) {
        deleteProcessedResponses(responsesToBeDeleted);
      }
    }

    deleteProcessedResponses(responsesToBeDeleted);
  }

  private boolean deleteProcessedResponses(Set<String> responsesToBeDeleted) {
    if (isEmpty(responsesToBeDeleted)) {
      return true;
    }

    boolean deleteSuccessful =
        persistence.deleteOnServer(persistence.createQuery(DelegateTaskProgressResponse.class, excludeAuthority)
                                       .field(DelegateTaskProgressResponseKeys.uuid)
                                       .in(responsesToBeDeleted));

    if (deleteSuccessful) {
      responsesToBeDeleted.clear();
    }

    return deleteSuccessful;
  }
}
