package io.harness.ng.core.activityhistory;

import io.harness.ng.core.activityhistory.entity.NGActivity.ActivityHistoryEntityKeys;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class EntityActivityQueryCriteriaHelper {
  public void addTimeFilterInTheCriteria(Criteria criteria, long startTime, long endTime) {
    criteria.and(ActivityHistoryEntityKeys.activityTime).gte(startTime).lt(endTime);
  }

  public void populateEntityFQNFilterInCriteria(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier) {
    criteria.and(ActivityHistoryEntityKeys.referredEntityFQN)
        .is(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier));
  }
}
