package io.harness.ng.core.entityReference;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import io.harness.ng.core.entityReference.entity.EntityReference.EntityReferenceKeys;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class EntityReferenceFilterHelper {
  public Criteria createCriteriaFromEntityFilter(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, String searchTerm) {
    Criteria criteria = new Criteria();
    criteria.and(EntityReferenceKeys.referredEntityFQN)
        .is(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier));
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(EntityReferenceKeys.referredByEntityName).regex(searchTerm),
          Criteria.where(EntityReferenceKeys.referredEntityName).regex(searchTerm));
    }
    return criteria;
  }
}
