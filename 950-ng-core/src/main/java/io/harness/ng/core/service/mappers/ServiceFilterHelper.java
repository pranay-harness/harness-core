package io.harness.ng.core.service.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.entity.ServiceEntity.ServiceEntityKeys;
import io.harness.ng.core.utils.CoreCriteriaUtils;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
@UtilityClass
public class ServiceFilterHelper {
  public Criteria createCriteriaForGetList(
      String accountId, String orgIdentifier, String projectIdentifier, boolean deleted, String searchTerm) {
    Criteria criteria =
        CoreCriteriaUtils.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier, deleted);
    if (isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(ServiceEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(ServiceEntityKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  public Update getUpdateOperations(ServiceEntity serviceEntity) {
    Update update = new Update();
    update.set(ServiceEntityKeys.accountId, serviceEntity.getAccountId());
    update.set(ServiceEntityKeys.orgIdentifier, serviceEntity.getOrgIdentifier());
    update.set(ServiceEntityKeys.projectIdentifier, serviceEntity.getProjectIdentifier());
    update.set(ServiceEntityKeys.identifier, serviceEntity.getIdentifier());
    update.set(ServiceEntityKeys.name, serviceEntity.getName());
    update.set(ServiceEntityKeys.description, serviceEntity.getDescription());
    update.set(ServiceEntityKeys.tags, serviceEntity.getTags());
    update.set(ServiceEntityKeys.deleted, false);
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(ServiceEntityKeys.deleted, true);
    return update;
  }
}
