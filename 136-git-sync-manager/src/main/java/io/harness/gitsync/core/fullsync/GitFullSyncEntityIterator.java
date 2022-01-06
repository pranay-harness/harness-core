/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;
import static org.springframework.data.domain.Sort.Direction.ASC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob.GitFullSyncJobKeys;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob.SyncStatus;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitFullSyncEntityIterator implements Handler<GitFullSyncJob> {
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final MongoTemplate mongoTemplate;
  private final GitFullSyncProcessorService gitFullSyncProcessorService;

  public void registerIterators(int threadPoolSize) {
    SpringFilterExpander filterExpander = getFilterQuery();
    registerIteratorWithFactory(threadPoolSize, filterExpander);
  }

  @Override
  public void handle(GitFullSyncJob entity) {
    gitFullSyncProcessorService.performFullSync(entity);
  }

  private void registerIteratorWithFactory(int threadPoolSize, @NotNull SpringFilterExpander filterExpander) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(threadPoolSize)
            .interval(ofSeconds(5))
            .build(),
        GitFullSyncJob.class,
        MongoPersistenceIterator.<GitFullSyncJob, SpringFilterExpander>builder()
            .clazz(GitFullSyncJob.class)
            .fieldName(GitFullSyncJobKeys.nextRuntime)
            .targetInterval(ofSeconds(120))
            .acceptableExecutionTime(ofSeconds(240))
            .acceptableNoAlertDelay(ofSeconds(300))
            .filterExpander(filterExpander)
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  private SpringFilterExpander getFilterQuery() {
    return query -> {
      Criteria criteria = Criteria.where(GitFullSyncJobKeys.syncStatus)
                              .in(Arrays.asList(SyncStatus.QUEUED.name(), SyncStatus.FAILED_WITH_RETRIES_LEFT.name()));
      query.addCriteria(criteria);
      query.with(Sort.by(ASC, GitFullSyncJobKeys.lastUpdatedAt));
      query.with(PageRequest.of(0, 1));
    };
  }
}
