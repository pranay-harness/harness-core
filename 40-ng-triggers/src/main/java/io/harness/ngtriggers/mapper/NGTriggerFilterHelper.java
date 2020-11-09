package io.harness.ngtriggers.mapper;

import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@UtilityClass
public class NGTriggerFilterHelper {
  public Criteria createCriteriaForGetList(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String targetIdentifier, NGTriggerType type, String searchTerm, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountIdentifier)) {
      criteria.and(NGTriggerEntityKeys.accountId).is(accountIdentifier);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(NGTriggerEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(NGTriggerEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (isNotEmpty(targetIdentifier)) {
      criteria.and(NGTriggerEntityKeys.targetIdentifier).is(targetIdentifier);
    }
    criteria.and(NGTriggerEntityKeys.deleted).is(deleted);

    if (type != null) {
      criteria.and(NGTriggerEntityKeys.type).is(type);
    }
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(NGTriggerEntityKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(NGTriggerEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  public Update getUpdateOperations(NGTriggerEntity triggerEntity) {
    Update update = new Update();
    update.set(NGTriggerEntityKeys.name, triggerEntity.getName());
    update.set(NGTriggerEntityKeys.identifier, triggerEntity.getIdentifier());
    update.set(NGTriggerEntityKeys.description, triggerEntity.getDescription());
    update.set(NGTriggerEntityKeys.yaml, triggerEntity.getYaml());

    update.set(NGTriggerEntityKeys.type, triggerEntity.getType());
    update.set(NGTriggerEntityKeys.metadata, triggerEntity.getMetadata());

    update.set(NGTriggerEntityKeys.deleted, false);

    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(NGTriggerEntityKeys.deleted, true);
    return update;
  }
}
