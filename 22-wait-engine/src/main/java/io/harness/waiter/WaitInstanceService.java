package io.harness.waiter;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoSerializer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.WriteConcern;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class WaitInstanceService {
  @Inject private HPersistence persistence;

  private static final Duration MAX_CALLBACK_PROCESSING_TIME = Duration.ofMinutes(1);

  private FindAndModifyOptions findAndModifyOptions =
      new FindAndModifyOptions().writeConcern(WriteConcern.MAJORITY).upsert(false).returnNew(false);

  public WaitInstance fetchForProcessingWaitInstance(String waitInstanceId, long now) {
    final Query<WaitInstance> waitInstanceQuery = persistence.createQuery(WaitInstance.class)
                                                      .filter(WaitInstance.WaitInstanceKeys.uuid, waitInstanceId)
                                                      .field(WaitInstance.WaitInstanceKeys.callbackProcessingAt)
                                                      .lessThan(now);

    final UpdateOperations<WaitInstance> updateOperations =
        persistence.createUpdateOperations(WaitInstance.class)
            .set(WaitInstance.WaitInstanceKeys.callbackProcessingAt, now + MAX_CALLBACK_PROCESSING_TIME.toMillis());

    return persistence.findAndModify(waitInstanceQuery, updateOperations, findAndModifyOptions);
  }

  public ProgressUpdate fetchForProcessingProgressUpdate(Set<String> busyCorrelationIds, long now) {
    Query<ProgressUpdate> query = null;
    if (busyCorrelationIds.isEmpty()) {
      query = persistence.createQuery(ProgressUpdate.class, excludeAuthority)
                  .order(ProgressUpdate.ProgressUpdateKeys.createdAt);
    } else {
      query = persistence.createQuery(ProgressUpdate.class, excludeAuthority)
                  .field(ProgressUpdate.ProgressUpdateKeys.correlationId)
                  .notIn(busyCorrelationIds)
                  .order(ProgressUpdate.ProgressUpdateKeys.createdAt);
    }

    UpdateOperations<ProgressUpdate> updateOperations =
        persistence.createUpdateOperations(ProgressUpdate.class)
            .set(ProgressUpdate.ProgressUpdateKeys.expireProcessing,
                now + WaitInstanceService.MAX_CALLBACK_PROCESSING_TIME.toMillis());

    return persistence.findAndModify(query, updateOperations, findAndModifyOptions);
  }

  public void checkProcessingTime(long startTime) {
    final long passed = System.currentTimeMillis() - startTime;
    if (passed > MAX_CALLBACK_PROCESSING_TIME.toMillis()) {
      log.error("It took more than {} ms before we processed the callback. THIS IS VERY BAD!!!",
          MAX_CALLBACK_PROCESSING_TIME.toMillis());
    }
  }
}
