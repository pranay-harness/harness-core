package io.harness.repositories.ng.core.custom;

import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory.TriggerEventHistoryKeys;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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
    return mongoTemplate.find(query, TriggerEventHistory.class);
  }

  @Override
  public List<TriggerEventHistory> findAllActivationTimestampsInRange(Criteria criteria) {
    Query query = new Query(criteria);
    query.fields()
        .include(TriggerEventHistoryKeys.uuid)
        .include(TriggerEventHistoryKeys.createdAt)
        .include(TriggerEventHistoryKeys.exceptionOccurred);
    return mongoTemplate.find(query, TriggerEventHistory.class);
  }
}
