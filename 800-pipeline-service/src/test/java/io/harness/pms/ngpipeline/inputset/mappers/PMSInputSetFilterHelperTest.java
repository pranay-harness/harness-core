package io.harness.pms.ngpipeline.inputset.mappers;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetListTypePMS;
import io.harness.rule.Owner;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

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
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
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
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
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
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.inputSetEntityType))
        .isEqualTo(InputSetEntityType.INPUT_SET);

    criteriaFromFilter = PMSInputSetFilterHelper.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier,
        pipelineIdentifier, InputSetListTypePMS.OVERLAY_INPUT_SET, "", false);
    assertThat(criteriaFromFilter).isNotNull();

    criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(InputSetEntity.InputSetEntityKeys.inputSetEntityType))
        .isEqualTo(InputSetEntityType.OVERLAY_INPUT_SET);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    InputSetEntity inputSetEntity = InputSetEntity.builder().build();
    inputSetEntity.setInputSetEntityType(InputSetEntityType.INPUT_SET);
    Update updateOperations = PMSInputSetFilterHelper.getUpdateOperations(inputSetEntity);
    Set<String> stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(InputSetEntity.class);
    Set<String> excludedFields = new HashSet<>(Arrays.asList(InputSetEntity.InputSetEntityKeys.uuid,
        InputSetEntity.InputSetEntityKeys.createdAt, InputSetEntity.InputSetEntityKeys.lastUpdatedAt,
        InputSetEntity.InputSetEntityKeys.version, InputSetEntity.InputSetEntityKeys.inputSetReferences,
        InputSetEntity.InputSetEntityKeys.inputSetEntityType, InputSetEntity.InputSetEntityKeys.deleted, "class"));

    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      boolean shouldExist =
          stringSet.contains(propertyDescriptor.getName()) || excludedFields.contains(propertyDescriptor.getName());
      assertThat(shouldExist).isTrue();

      boolean onlyOnce =
          stringSet.contains(propertyDescriptor.getName()) ^ excludedFields.contains(propertyDescriptor.getName());
      assertThat(onlyOnce).isTrue();
    }

    inputSetEntity.setInputSetEntityType(InputSetEntityType.OVERLAY_INPUT_SET);
    updateOperations = PMSInputSetFilterHelper.getUpdateOperations(inputSetEntity);
    stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    propertyDescriptors = BeanUtils.getPropertyDescriptors(InputSetEntity.class);
    excludedFields =
        new HashSet<>(Arrays.asList(InputSetEntity.InputSetEntityKeys.uuid, InputSetEntity.InputSetEntityKeys.createdAt,
            InputSetEntity.InputSetEntityKeys.lastUpdatedAt, InputSetEntity.InputSetEntityKeys.version,
            InputSetEntity.InputSetEntityKeys.inputSetEntityType, InputSetEntity.InputSetEntityKeys.deleted, "class"));

    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      boolean shouldExist =
          stringSet.contains(propertyDescriptor.getName()) || excludedFields.contains(propertyDescriptor.getName());
      assertThat(shouldExist).isTrue();
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetUpdateOperationsForDelete() {
    Update updateOperations = PMSInputSetFilterHelper.getUpdateOperationsForDelete();
    Set<String> stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(InputSetEntity.class);

    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      if (propertyDescriptor.getName().equals("deleted")) {
        assertThat(stringSet.contains(propertyDescriptor.getName())).isTrue();
      } else {
        assertThat(stringSet.contains(propertyDescriptor.getName())).isFalse();
      }
    }
  }
}