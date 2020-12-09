package io.harness.repositories.ng.core.custom;

import io.harness.ngtriggers.beans.entity.TriggerEventHistory;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class TriggerEventHistoryRepositoryCustomImpl implements TriggerEventHistoryRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<TriggerEventHistory> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    List<TriggerEventHistory> triggersList = mongoTemplate.find(query, TriggerEventHistory.class);
    return triggersList;
  }

  @Override
  public TriggerEventHistory findLatest(Criteria criteria) {
    Query query = new Query(criteria);
    TriggerEventHistory latestTriggerExecution = mongoTemplate.findOne(query, TriggerEventHistory.class);
    return latestTriggerExecution;
  }
}
