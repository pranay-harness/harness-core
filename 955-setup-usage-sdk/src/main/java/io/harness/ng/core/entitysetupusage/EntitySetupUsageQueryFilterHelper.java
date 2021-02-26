package io.harness.ng.core.entitysetupusage;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;

import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class EntitySetupUsageQueryFilterHelper {
  public Criteria createCriteriaFromEntityFilter(
      String accountIdentifier, String referredEntityFQN, String searchTerm) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredEntityFQN).is(referredEntityFQN);
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(EntitySetupUsageKeys.referredEntityFQN).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredByEntityFQN).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredEntityName).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredByEntityName).regex(searchTerm));
    }
    return criteria;
  }
}
