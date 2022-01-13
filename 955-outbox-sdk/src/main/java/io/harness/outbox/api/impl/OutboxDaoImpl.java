/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.outbox.api.impl;

import static io.harness.NGCommonEntityConstants.MONGODB_ID;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_CREATED_AT_ASC_SORT_ORDER;
import static io.harness.utils.PageUtils.getPageRequest;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;
import io.harness.outbox.api.OutboxDao;
import io.harness.outbox.filter.OutboxEventFilter;
import io.harness.outbox.filter.OutboxEventsPerEventTypeCount;
import io.harness.outbox.filter.OutboxEventsPerEventTypeCount.OutboxEventsPerEventTypeCountKeys;
import io.harness.outbox.filter.OutboxMetricsFilter;
import io.harness.repositories.outbox.OutboxEventRepository;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.Assert;

@OwnedBy(PL)
public class OutboxDaoImpl implements OutboxDao {
  private final OutboxEventRepository outboxRepository;

  @Inject
  public OutboxDaoImpl(OutboxEventRepository outboxRepository) {
    this.outboxRepository = outboxRepository;
  }

  @Override
  public OutboxEvent save(OutboxEvent outboxEvent) {
    return outboxRepository.save(outboxEvent);
  }

  @Override
  public List<OutboxEvent> list(OutboxEventFilter outboxEventFilter) {
    Assert.notNull(outboxEventFilter, "OutboxEventFilter must not be null!");
    return outboxRepository.findAll(getCriteria(outboxEventFilter),
        getPageRequest(PageRequest.builder()
                           .pageIndex(0)
                           .pageSize(outboxEventFilter.getMaximumEventsPolled())
                           .sortOrders(DEFAULT_CREATED_AT_ASC_SORT_ORDER)
                           .build()));
  }

  @Override
  public long count(OutboxMetricsFilter outboxMetricsFilter) {
    Criteria criteria = new Criteria();
    if (outboxMetricsFilter != null && outboxMetricsFilter.getBlocked() != null) {
      criteria = criteria.and(OutboxEventKeys.blocked).is(outboxMetricsFilter.getBlocked());
    }
    return outboxRepository.count(criteria);
  }

  @Override
  public Map<String, Long> countPerEventType(OutboxMetricsFilter outboxMetricsFilter) {
    Criteria criteria = new Criteria();
    if (outboxMetricsFilter != null && outboxMetricsFilter.getBlocked() != null) {
      criteria = criteria.and(OutboxEventKeys.blocked).is(outboxMetricsFilter.getBlocked());
    }
    MatchOperation matchStage = Aggregation.match(criteria);
    SortOperation sortStage = sort(Sort.by(OutboxEventKeys.eventType));
    GroupOperation groupByOrganizationStage =
        group(OutboxEventKeys.eventType).count().as(OutboxEventsPerEventTypeCountKeys.count);
    ProjectionOperation projectionStage =
        project().and(MONGODB_ID).as(OutboxEventKeys.eventType).andInclude(OutboxEventsPerEventTypeCountKeys.count);
    Map<String, Long> result = new HashMap<>();
    outboxRepository
        .aggregate(newAggregation(matchStage, sortStage, groupByOrganizationStage, projectionStage),
            OutboxEventsPerEventTypeCount.class)
        .getMappedResults()
        .forEach(outboxEventsPerEventTypeCount
            -> result.put(outboxEventsPerEventTypeCount.getEventType(), outboxEventsPerEventTypeCount.getCount()));
    return result;
  }

  private Criteria getCriteria(OutboxEventFilter outboxEventFilter) {
    Criteria criteria = new Criteria();
    Criteria blockedNotTrueCriteria = Criteria.where(OutboxEventKeys.blocked).ne(Boolean.TRUE);
    Criteria blockedTrueCriteria = Criteria.where(OutboxEventKeys.blocked)
                                       .is(Boolean.TRUE)
                                       .and(OutboxEventKeys.nextUnblockAttemptAt)
                                       .lt(Instant.now());
    criteria.orOperator(blockedNotTrueCriteria, blockedTrueCriteria);
    return criteria;
  }

  @Override
  public boolean delete(String outboxEventId) {
    outboxRepository.deleteById(outboxEventId);
    return true;
  }
}
