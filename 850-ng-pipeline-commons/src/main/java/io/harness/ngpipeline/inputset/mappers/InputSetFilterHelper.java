package io.harness.ngpipeline.inputset.mappers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity.CDInputSetEntityKeys;
import io.harness.ngpipeline.inputset.beans.resource.InputSetListType;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity.OverlayInputSetEntityKeys;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
@ToBeDeleted
@Deprecated
public class InputSetFilterHelper {
  public Criteria createCriteriaForGetList(String accountId, String orgIdentifier, String projectIdentifier,
      String pipeIdentifier, InputSetListType type, String searchTerm, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(BaseInputSetEntityKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(BaseInputSetEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(BaseInputSetEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (isNotEmpty(pipeIdentifier)) {
      criteria.and(BaseInputSetEntityKeys.pipelineIdentifier).is(pipeIdentifier);
    }
    criteria.and(BaseInputSetEntityKeys.deleted).is(deleted);

    if (type != InputSetListType.ALL) {
      criteria.and(BaseInputSetEntityKeys.inputSetType).is(getInputSetType(type));
    }

    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria =
          new Criteria().orOperator(where(BaseInputSetEntityKeys.name)
                                        .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
              where(BaseInputSetEntityKeys.identifier)
                  .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }

    return criteria;
  }

  public Update getUpdateOperations(BaseInputSetEntity baseInputSetEntity) {
    Update update = new Update();
    update.set(BaseInputSetEntityKeys.accountId, baseInputSetEntity.getAccountId());
    update.set(BaseInputSetEntityKeys.orgIdentifier, baseInputSetEntity.getOrgIdentifier());
    update.set(BaseInputSetEntityKeys.projectIdentifier, baseInputSetEntity.getProjectIdentifier());
    update.set(BaseInputSetEntityKeys.pipelineIdentifier, baseInputSetEntity.getPipelineIdentifier());
    update.set(BaseInputSetEntityKeys.identifier, baseInputSetEntity.getIdentifier());
    update.set(BaseInputSetEntityKeys.inputSetYaml, baseInputSetEntity.getInputSetYaml());
    update.set(BaseInputSetEntityKeys.name, baseInputSetEntity.getName());
    update.set(BaseInputSetEntityKeys.description, baseInputSetEntity.getDescription());
    update.set(BaseInputSetEntityKeys.tags, baseInputSetEntity.getTags());
    update.set(BaseInputSetEntityKeys.referredEntities, baseInputSetEntity.getReferredEntities());
    update.set(BaseInputSetEntityKeys.deleted, false);

    if (baseInputSetEntity.getInputSetType() == InputSetEntityType.INPUT_SET) {
      getUpdateOperationsInputSet((InputSetEntity) baseInputSetEntity, update);
    } else if (baseInputSetEntity.getInputSetType() == InputSetEntityType.OVERLAY_INPUT_SET) {
      getUpdateOperationsOverlayInputSet((OverlayInputSetEntity) baseInputSetEntity, update);
    }

    return update;
  }

  private void getUpdateOperationsInputSet(InputSetEntity inputSetEntity, Update update) {
    update.set(CDInputSetEntityKeys.inputSetConfig, inputSetEntity.getInputSetConfig());
    update.set(BaseInputSetEntityKeys.inputSetType, InputSetEntityType.INPUT_SET);
  }

  private void getUpdateOperationsOverlayInputSet(OverlayInputSetEntity overlayInputSetEntity, Update update) {
    update.set(BaseInputSetEntityKeys.inputSetType, InputSetEntityType.OVERLAY_INPUT_SET);
    update.set(OverlayInputSetEntityKeys.inputSetReferences, overlayInputSetEntity.getInputSetReferences());
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(BaseInputSetEntityKeys.deleted, true);
    return update;
  }

  private InputSetEntityType getInputSetType(InputSetListType inputSetListType) {
    if (inputSetListType == InputSetListType.INPUT_SET) {
      return InputSetEntityType.INPUT_SET;
    }
    return InputSetEntityType.OVERLAY_INPUT_SET;
  }
}
