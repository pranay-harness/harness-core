/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.environment.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;
import io.harness.ng.core.utils.CoreCriteriaUtils;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
@UtilityClass
public class EnvironmentFilterHelper {
  public Criteria createCriteriaForGetList(
      String accountId, String orgIdentifier, String projectIdentifier, boolean deleted, String searchTerm) {
    Criteria criteria =
        CoreCriteriaUtils.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier, deleted);
    if (isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(EnvironmentKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(EnvironmentKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  public Update getUpdateOperations(Environment environment) {
    Update update = new Update();
    update.set(EnvironmentKeys.accountId, environment.getAccountId());
    update.set(EnvironmentKeys.orgIdentifier, environment.getOrgIdentifier());
    update.set(EnvironmentKeys.projectIdentifier, environment.getProjectIdentifier());
    update.set(EnvironmentKeys.identifier, environment.getIdentifier());
    update.set(EnvironmentKeys.name, environment.getName());
    update.set(EnvironmentKeys.description, environment.getDescription());
    update.set(EnvironmentKeys.type, environment.getType());
    update.set(EnvironmentKeys.deleted, false);
    update.set(EnvironmentKeys.tags, environment.getTags());
    update.set(EnvironmentKeys.color, environment.getColor());
    update.setOnInsert(EnvironmentKeys.createdAt, System.currentTimeMillis());
    update.set(EnvironmentKeys.lastModifiedAt, System.currentTimeMillis());
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(EnvironmentKeys.deleted, true);
    return update;
  }
}
