package io.harness.pms.ngpipeline.inputset.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetListTypePMS;
import io.harness.rule.Owner;

import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public class PMSInputSetFilterHelperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetCriteria() {
    String accountId = "ACCOUNT_ID";
    String orgIdentifier = "ORG_ID";
    String projectIdentifier = "PROJECT_ID";
    String pipelineIdentifier = "PIPELINE_ID";
    Criteria criteriaFromFilter = PMSInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, InputSetListTypePMS.ALL, null, false);
    assertThat(criteriaFromFilter).isNotNull();

    Document criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(InputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(InputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(InputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(InputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetCriteriaWithSearchTerm() {
    String accountId = "ACCOUNT_ID";
    String orgIdentifier = "ORG_ID";
    String projectIdentifier = "PROJECT_ID";
    String pipelineIdentifier = "PIPELINE_ID";
    String searchTerm = "overlay.*";
    Criteria criteriaFromFilter = PMSInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, InputSetListTypePMS.ALL, searchTerm, false);
    assertThat(criteriaFromFilter).isNotNull();

    Document criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(InputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(InputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(InputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(InputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
    assertThat(criteriaObject.get("$and")).isNotNull();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetCriteriaForOnlyOneKind() {
    String accountId = "ACCOUNT_ID";
    String orgIdentifier = "ORG_ID";
    String projectIdentifier = "PROJECT_ID";
    String pipelineIdentifier = "PIPELINE_ID";
    Criteria criteriaFromFilter = PMSInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, InputSetListTypePMS.INPUT_SET, "", false);
    assertThat(criteriaFromFilter).isNotNull();

    Document criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(InputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(InputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(InputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(InputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
    assertThat(criteriaObject.get(InputSetEntityKeys.inputSetEntityType)).isEqualTo(InputSetEntityType.INPUT_SET);

    criteriaFromFilter = PMSInputSetFilterHelper.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier,
        pipelineIdentifier, InputSetListTypePMS.OVERLAY_INPUT_SET, "", false);
    assertThat(criteriaFromFilter).isNotNull();

    criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(InputSetEntityKeys.inputSetEntityType))
        .isEqualTo(InputSetEntityType.OVERLAY_INPUT_SET);
  }
}