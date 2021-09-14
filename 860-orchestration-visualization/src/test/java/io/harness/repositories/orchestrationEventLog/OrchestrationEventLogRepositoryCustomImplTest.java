/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.orchestrationEventLog;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationEventLog;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventLogRepositoryCustomImplTest extends OrchestrationVisualizationTestBase {
  @Inject private MongoTemplate mongoTemplate;
  @Inject private OrchestrationEventLogRepositoryCustomImpl repository;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldUpdateTtlForProcessedEvents() {
    OrchestrationEventLog log = OrchestrationEventLog.builder().validUntil(Date.valueOf(LocalDate.now())).build();
    OrchestrationEventLog savedLog = mongoTemplate.save(log);
    List<OrchestrationEventLog> orchestrationEventLogs = ImmutableList.of(savedLog);

    repository.updateTtlForProcessedEvents(orchestrationEventLogs);

    OrchestrationEventLog foundLog = mongoTemplate.findById(savedLog.getId(), OrchestrationEventLog.class);
    assertThat(foundLog).isNotNull();
    assertThat(foundLog.getValidUntil())
        .isBeforeOrEqualTo(Date.from(OffsetDateTime.now().plus(Duration.ofMinutes(10)).toInstant()));
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestSchemaMigrationForOldEvenLog() {
    String planExecutionId = generateUuid();
    OrchestrationEventLog log = OrchestrationEventLog.builder()
                                    .planExecutionId(planExecutionId)
                                    .createdAt(System.currentTimeMillis())
                                    .validUntil(null)
                                    .build();
    OrchestrationEventLog log1 = OrchestrationEventLog.builder()
                                     .planExecutionId(planExecutionId)
                                     .createdAt(System.currentTimeMillis())
                                     .validUntil(null)
                                     .build();

    mongoTemplate.insertAll(ImmutableList.of(log, log1));

    List<OrchestrationEventLog> unprocessedEvents =
        repository.findUnprocessedEvents(planExecutionId, System.currentTimeMillis() - 5000);
    assertThat(unprocessedEvents).isNotNull();
    assertThat(unprocessedEvents.size()).isEqualTo(2);

    repository.schemaMigrationForOldEvenLog();

    List<OrchestrationEventLog> unprocessedEventsAfterMigration =
        repository.findUnprocessedEvents(planExecutionId, System.currentTimeMillis() - 5000);
    assertThat(unprocessedEventsAfterMigration).isNotNull();
    assertThat(unprocessedEventsAfterMigration).isEmpty();
  }
}
