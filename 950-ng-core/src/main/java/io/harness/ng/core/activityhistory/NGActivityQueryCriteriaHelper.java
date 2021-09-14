/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.activityhistory;

import io.harness.EntityType;
import io.harness.ng.core.activityhistory.entity.NGActivity.ActivityHistoryEntityKeys;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class NGActivityQueryCriteriaHelper {
  public void addTimeFilterInTheCriteria(Criteria criteria, long startTime, long endTime) {
    criteria.and(ActivityHistoryEntityKeys.activityTime).gte(startTime).lt(endTime);
  }

  public void populateEntityFQNFilterInCriteria(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier) {
    criteria.and(ActivityHistoryEntityKeys.referredEntityFQN)
        .is(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier));
  }

  public void addReferredEntityTypeCriteria(Criteria criteria, EntityType referredEntityType) {
    if (referredEntityType != null) {
      criteria.and(ActivityHistoryEntityKeys.referredEntityType).is(String.valueOf(referredEntityType));
    }
  }

  public void addReferredByEntityTypeCriteria(Criteria criteria, EntityType referredByEntityType) {
    if (referredByEntityType != null) {
      criteria.and(ActivityHistoryEntityKeys.referredByEntityType).is(String.valueOf(referredByEntityType));
    }
  }
}
