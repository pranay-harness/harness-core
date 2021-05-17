package io.harness.repositories.instance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instance.Instance;
import io.harness.entities.instance.Instance.InstanceKeys;

import com.google.inject.Inject;
import groovy.util.logging.Slf4j;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InstanceRepositoryCustomImpl implements InstanceRepositoryCustom {
  private MongoTemplate mongoTemplate;

  @Override
  public List<Instance> getActiveInstancesByAccount(String accountIdentifier, long timestamp) {
    Criteria criteria = Criteria.where(InstanceKeys.accountIdentifier).is(accountIdentifier);
    if (timestamp > 0) {
      criteria = criteria.andOperator(
          Criteria.where(InstanceKeys.createdAt)
              .lte(timestamp)
              .andOperator(Criteria.where(InstanceKeys.isDeleted)
                               .is(false)
                               .orOperator(Criteria.where(InstanceKeys.deletedAt).gte(timestamp))));
    } else {
      criteria = criteria.andOperator(Criteria.where(InstanceKeys.isDeleted).is(false));
    }

    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.find(query, Instance.class);
  }

  @Override
  public List<Instance> getInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId) {
    // TODO
    return null;
  }
}
