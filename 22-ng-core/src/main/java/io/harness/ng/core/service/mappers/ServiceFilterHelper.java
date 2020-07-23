package io.harness.ng.core.service.mappers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.ng.core.service.entity.ServiceEntity.ServiceEntityKeys;
import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;

@UtilityClass
public class ServiceFilterHelper {
  public Criteria createCriteria(String accountId, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(ServiceEntityKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(ServiceEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(ServiceEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    return criteria;
  }
}
