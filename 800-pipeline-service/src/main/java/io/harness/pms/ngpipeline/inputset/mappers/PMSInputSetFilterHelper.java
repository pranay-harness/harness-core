package io.harness.pms.ngpipeline.inputset.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetListTypePMS;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
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

  private InputSetEntityType getInputSetType(InputSetListTypePMS inputSetListType) {
    if (inputSetListType == InputSetListTypePMS.INPUT_SET) {
      return InputSetEntityType.INPUT_SET;
    }
    return InputSetEntityType.OVERLAY_INPUT_SET;
  }
}
