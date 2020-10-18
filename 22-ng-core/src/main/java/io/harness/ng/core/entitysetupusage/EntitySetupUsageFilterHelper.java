package io.harness.ng.core.entitysetupusage;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class EntitySetupUsageFilterHelper {
  public Criteria createCriteriaFromEntityFilter(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, String searchTerm) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.referredEntityFQN)
        .is(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier));
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(EntitySetupUsageKeys.referredByEntityName).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredEntityName).regex(searchTerm));
    }
    return criteria;
  }
}
