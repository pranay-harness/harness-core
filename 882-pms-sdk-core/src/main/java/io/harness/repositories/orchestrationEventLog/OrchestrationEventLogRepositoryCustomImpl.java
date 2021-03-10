package io.harness.repositories.orchestrationEventLog;

import io.harness.pms.sdk.core.events.OrchestrationEventLog;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

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
  public List<OrchestrationEventLog> findUnprocessedEvents(String planExecutionId) {
    Criteria criteria = Criteria.where("planExecutionId").is(planExecutionId);
    Query query = new Query(criteria).with(Sort.by(Sort.Order.asc("createdAt")));
    return mongoTemplate.find(query, OrchestrationEventLog.class);
  }
}
