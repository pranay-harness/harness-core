/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.orchestrationEventLog;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationEventLog;
import io.harness.beans.OrchestrationEventLog.OrchestrationEventLogKeys;

import com.google.inject.Inject;
import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class OrchestrationEventLogRepositoryCustomImpl implements OrchestrationEventLogRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<OrchestrationEventLog> findUnprocessedEvents(String planExecutionId, long lastUpdatedAt) {
    Criteria criteria = Criteria.where("planExecutionId").is(planExecutionId);
    criteria.andOperator(Criteria.where("createdAt").gt(lastUpdatedAt));
    Query query = new Query(criteria).with(Sort.by(Sort.Order.asc("createdAt")));
    return mongoTemplate.find(query, OrchestrationEventLog.class);
  }

  @Override
  public void updateTtlForProcessedEvents(List<OrchestrationEventLog> eventLogs) {
    List<String> ids = eventLogs.stream().map(OrchestrationEventLog::getId).collect(Collectors.toList());
    Criteria criteria = Criteria.where(OrchestrationEventLogKeys.id).in(ids);
    Update update = new Update();
    // Setting a ttl of 10 minutes so that we if there is a race condition between multiple replicas while updating,
    // then graph should not be in inconsistent state
    update.set(
        OrchestrationEventLogKeys.validUntil, Date.from(OffsetDateTime.now().plus(Duration.ofMinutes(10)).toInstant()));
    mongoTemplate.updateMulti(new Query(criteria), update, OrchestrationEventLog.class);
  }

  @Override
  public void schemaMigrationForOldEvenLog() {
    Criteria criteria = Criteria.where(OrchestrationEventLogKeys.validUntil).exists(false);
    mongoTemplate.remove(new Query(criteria), OrchestrationEventLog.class);
  }
}
