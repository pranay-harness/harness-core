/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aggregator;

import io.harness.aggregator.models.MongoReconciliationOffset;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
public class MongoOffsetCleanupJob implements Managed {
  private final ScheduledExecutorService executorService;
  private final MongoTemplate mongoTemplate;

  @Inject
  public MongoOffsetCleanupJob(MongoTemplate mongoTemplate) {
    executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("mongo-offset-cleanup-job").build());
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void start() {
    executorService.scheduleWithFixedDelay(() -> {
      log.info("Running Mongo offset cleanup job...");
      long currentTimeMillis = System.currentTimeMillis();
      long purgeBeforeTime = currentTimeMillis - TimeUnit.DAYS.toMillis(30);
      try {
        mongoTemplate.remove(new Query(Criteria.where(MongoReconciliationOffset.keys.createdAt).lte(purgeBeforeTime)),
            MongoReconciliationOffset.class);
      } catch (Exception exception) {
        log.error("Exception occurred while trying to clean mongo offsets", exception);
      }
    }, 1, 12 * 60L, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws Exception {
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);
  }
}
