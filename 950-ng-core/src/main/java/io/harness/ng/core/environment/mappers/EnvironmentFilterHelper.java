package io.harness.ng.core.environment.mappers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class EnvironmentFilterHelper {
  public Criteria createCriteriaForGetList(
      String accountId, String orgIdentifier, String projectIdentifier, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(EnvironmentKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(EnvironmentKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(EnvironmentKeys.projectIdentifier).is(projectIdentifier);
    }
    criteria.and(EnvironmentKeys.deleted).is(deleted);
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
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(EnvironmentKeys.deleted, true);
    return update;
  }
}
