package io.harness.ngpipeline.overlayinputset.mappers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity.OverlayInputSetEntityKeys;
import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class OverlayInputSetFilterHelper {
  public Criteria createCriteriaForGetList(
      String accountId, String orgIdentifier, String projectIdentifier, String pipeIdentifier, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(OverlayInputSetEntityKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(OverlayInputSetEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(OverlayInputSetEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (isNotEmpty(pipeIdentifier)) {
      criteria.and(OverlayInputSetEntityKeys.pipelineIdentifier).is(pipeIdentifier);
    }
    criteria.and(OverlayInputSetEntityKeys.deleted).is(deleted);
    return criteria;
  }

  public Update getUpdateOperations(OverlayInputSetEntity overlayInputSetEntity) {
    Update update = new Update();
    update.set(OverlayInputSetEntityKeys.accountId, overlayInputSetEntity.getAccountId());
    update.set(OverlayInputSetEntityKeys.orgIdentifier, overlayInputSetEntity.getOrgIdentifier());
    update.set(OverlayInputSetEntityKeys.projectIdentifier, overlayInputSetEntity.getProjectIdentifier());
    update.set(OverlayInputSetEntityKeys.pipelineIdentifier, overlayInputSetEntity.getPipelineIdentifier());
    update.set(OverlayInputSetEntityKeys.identifier, overlayInputSetEntity.getIdentifier());
    update.set(OverlayInputSetEntityKeys.overlayInputSetYaml, overlayInputSetEntity.getOverlayInputSetYaml());
    update.set(OverlayInputSetEntityKeys.name, overlayInputSetEntity.getName());
    update.set(OverlayInputSetEntityKeys.description, overlayInputSetEntity.getDescription());
    update.set(OverlayInputSetEntityKeys.inputSetsReferenceList, overlayInputSetEntity.getInputSetsReferenceList());
    update.set(OverlayInputSetEntityKeys.deleted, false);
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(OverlayInputSetEntityKeys.deleted, true);
    return update;
  }
}
