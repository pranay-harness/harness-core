package io.harness.cdng.inputset.mappers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity.CDInputSetEntityKeys;
import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class CDInputSetFilterHelper {
  public Criteria createCriteriaForGetList(
      String accountId, String orgIdentifier, String projectIdentifier, String pipeIdentifier, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(CDInputSetEntityKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(CDInputSetEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(CDInputSetEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (isNotEmpty(pipeIdentifier)) {
      criteria.and(CDInputSetEntityKeys.pipelineIdentifier).is(pipeIdentifier);
    }
    criteria.and(CDInputSetEntityKeys.deleted).is(deleted);
    return criteria;
  }

  public Update getUpdateOperations(CDInputSetEntity cdInputSetEntity) {
    Update update = new Update();
    update.set(CDInputSetEntityKeys.accountId, cdInputSetEntity.getAccountId());
    update.set(CDInputSetEntityKeys.orgIdentifier, cdInputSetEntity.getOrgIdentifier());
    update.set(CDInputSetEntityKeys.projectIdentifier, cdInputSetEntity.getProjectIdentifier());
    update.set(CDInputSetEntityKeys.pipelineIdentifier, cdInputSetEntity.getPipelineIdentifier());
    update.set(CDInputSetEntityKeys.identifier, cdInputSetEntity.getIdentifier());
    update.set(CDInputSetEntityKeys.cdInputSet, cdInputSetEntity.getCdInputSet());
    update.set(CDInputSetEntityKeys.inputSetYaml, cdInputSetEntity.getInputSetYaml());
    update.set(CDInputSetEntityKeys.name, cdInputSetEntity.getName());
    update.set(CDInputSetEntityKeys.description, cdInputSetEntity.getDescription());
    update.set(CDInputSetEntityKeys.deleted, false);
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(CDInputSetEntityKeys.deleted, true);
    return update;
  }
}
