package io.harness.instancesync.repository.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.instancesync.dto.Instance;
import io.harness.instancesync.repository.instancesyncperpetualtask.InstanceRepository;

import com.google.inject.Inject;
import groovy.util.logging.Slf4j;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InstanceRepositoryImpl implements InstanceRepository {
  private MongoTemplate mongoTemplate;

  @Override
  public List<Instance> getActiveInstancesByAccount(String accountId, long timestamp) {
    Criteria criteria = Criteria.where(Instance.ACCOUNT_ID_FIELD).is(accountId);
    if (timestamp > 0) {
      criteria = criteria.andOperator(
          Criteria.where(Instance.CREATED_AT_FIELD)
              .lte(timestamp)
              .andOperator(Criteria.where(Instance.IS_DELETED_FIELD)
                               .is(false)
                               .orOperator(Criteria.where(Instance.DELETED_AT_FIELD).gte(timestamp))));
    } else {
      criteria = criteria.andOperator(Criteria.where(Instance.IS_DELETED_FIELD).is(false));
    }

    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.find(query, Instance.class);
  }
}
