package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ProgressData;
import io.harness.waiter.ProgressUpdate.ProgressUpdateKeys;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class ProgressUpdateService implements Runnable {
  @Inject private Injector injector;
  @Inject private HPersistence persistence;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private WaitInstanceService waitInstanceService;

  @Override
  public void run() {
    Set<String> busyCorrelationIds = new HashSet<>();
    while (true) {
      try {
        final long now = System.currentTimeMillis();
        ProgressUpdate progressUpdate = waitInstanceService.fetchForProcessingProgressUpdate(busyCorrelationIds, now);
        if (progressUpdate == null) {
          break;
        }

        if (progressUpdate.getExpireProcessing() > now) {
          continue;
        }

        Query<WaitInstance> query = persistence.createQuery(WaitInstance.class, excludeAuthority)
                                        .filter(WaitInstanceKeys.correlationIds, progressUpdate.getCorrelationId())
                                        .project(WaitInstanceKeys.progressCallback, true);

        ProgressData progressData = (ProgressData) kryoSerializer.asInflatedObject(progressUpdate.getProgressData());

        try (HIterator<WaitInstance> iterator = new HIterator<>(query.fetch())) {
          for (WaitInstance waitInstance : iterator) {
            ProgressCallback progressCallback = waitInstance.getProgressCallback();
            injector.injectMembers(progressCallback);
            progressCallback.notify(progressUpdate.getCorrelationId(), progressData);
          }
        }
        persistence.delete(progressUpdate);
      } catch (Exception e) {
        log.error("Exception occurred while running progress service", e);
      }
    }
  }
}
