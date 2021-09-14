/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo.GitFullSyncEntityInfoKeys;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo.SyncStatus;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitFullSyncEntityIterator implements Handler<GitFullSyncEntityInfo> {
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final MongoTemplate mongoTemplate;

  public void registerIterators() {
    SpringFilterExpander filterExpander = getFilterQuery();
    registerIteratorWithFactory(filterExpander);
  }

  @Override
  public void handle(GitFullSyncEntityInfo entity) {
    // todo(abhinav): implement this
  }

  private void registerIteratorWithFactory(@NotNull SpringFilterExpander filterExpander) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(5)
            .interval(ofSeconds(5))
            .build(),
        GitFullSyncEntityInfo.class,
        MongoPersistenceIterator.<GitFullSyncEntityInfo, SpringFilterExpander>builder()
            .clazz(GitFullSyncEntityInfo.class)
            .fieldName(GitFullSyncEntityInfoKeys.nextRuntime)
            .targetInterval(ofSeconds(120))
            .acceptableExecutionTime(ofSeconds(100))
            .acceptableNoAlertDelay(ofSeconds(120))
            .filterExpander(filterExpander)
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  private SpringFilterExpander getFilterQuery() {
    return query -> {
      Criteria criteria = Criteria.where(GitFullSyncEntityInfoKeys.syncStatus).is(SyncStatus.QUEUED.name());
      query.addCriteria(criteria);
    };
  }
}
