package io.harness.pms.ngpipeline.inputset.mappers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetListTypePMS;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class PMSInputSetFilterHelper {
  public Criteria createCriteriaForGetList(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, InputSetListTypePMS type, String searchTerm, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(InputSetEntityKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(InputSetEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(InputSetEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (isNotEmpty(pipelineIdentifier)) {
      criteria.and(InputSetEntityKeys.pipelineIdentifier).is(pipelineIdentifier);
    }
    criteria.and(InputSetEntityKeys.deleted).is(deleted);

    if (type != InputSetListTypePMS.ALL) {
      criteria.and(InputSetEntityKeys.inputSetEntityType).is(getInputSetType(type));
    }

    if (isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(InputSetEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(InputSetEntityKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }

    return criteria;
  }

  public Update getUpdateOperations(InputSetEntity inputSetEntity) {
    Update update = new Update();
    update.set(InputSetEntityKeys.accountId, inputSetEntity.getAccountId());
    update.set(InputSetEntityKeys.orgIdentifier, inputSetEntity.getOrgIdentifier());
    update.set(InputSetEntityKeys.projectIdentifier, inputSetEntity.getProjectIdentifier());
    update.set(InputSetEntityKeys.pipelineIdentifier, inputSetEntity.getPipelineIdentifier());

    update.set(InputSetEntityKeys.identifier, inputSetEntity.getIdentifier());
    update.set(InputSetEntityKeys.name, inputSetEntity.getName());
    update.set(InputSetEntityKeys.description, inputSetEntity.getDescription());
    update.set(InputSetEntityKeys.tags, inputSetEntity.getTags());

    update.set(InputSetEntityKeys.yaml, inputSetEntity.getYaml());
    if (inputSetEntity.getInputSetEntityType() == InputSetEntityType.OVERLAY_INPUT_SET) {
      update.set(InputSetEntityKeys.inputSetReferences, inputSetEntity.getInputSetReferences());
    }
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(InputSetEntityKeys.deleted, true);
    return update;
  }

  private InputSetEntityType getInputSetType(InputSetListTypePMS inputSetListType) {
    if (inputSetListType == InputSetListTypePMS.INPUT_SET) {
      return InputSetEntityType.INPUT_SET;
    }
    return InputSetEntityType.OVERLAY_INPUT_SET;
  }
}
