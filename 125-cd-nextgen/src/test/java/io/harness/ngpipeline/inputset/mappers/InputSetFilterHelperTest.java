package io.harness.ngpipeline.inputset.mappers;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.resource.InputSetListType;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
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

public class InputSetFilterHelperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetCriteria() {
    String accountId = "ACCOUNT_ID";
    String orgIdentifier = "ORG_ID";
    String projectIdentifier = "PROJECT_ID";
    String pipelineIdentifier = "PIPELINE_ID";
    Criteria criteriaFromFilter = InputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, InputSetListType.ALL, null, false);
    assertThat(criteriaFromFilter).isNotNull();

    Document criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
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
    Criteria criteriaFromFilter = InputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, InputSetListType.ALL, searchTerm, false);
    assertThat(criteriaFromFilter).isNotNull();

    Document criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
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
    Criteria criteriaFromFilter = InputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, InputSetListType.INPUT_SET, "", false);
    assertThat(criteriaFromFilter).isNotNull();

    Document criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.inputSetType)).isEqualTo(InputSetEntityType.INPUT_SET);

    criteriaFromFilter = InputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, InputSetListType.OVERLAY_INPUT_SET, "", false);
    assertThat(criteriaFromFilter).isNotNull();

    criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(BaseInputSetEntityKeys.inputSetType)).isEqualTo(InputSetEntityType.OVERLAY_INPUT_SET);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    InputSetEntity inputSetEntity = InputSetEntity.builder().build();
    inputSetEntity.setInputSetType(InputSetEntityType.INPUT_SET);
    Update updateOperations = InputSetFilterHelper.getUpdateOperations(inputSetEntity);
    Set<String> stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(InputSetEntity.class);
    Set<String> excludedFields =
        new HashSet<>(Arrays.asList(BaseInputSetEntityKeys.id, BaseInputSetEntityKeys.createdAt,
            BaseInputSetEntityKeys.lastModifiedAt, BaseInputSetEntityKeys.version, "class"));

    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      boolean shouldExist =
          stringSet.contains(propertyDescriptor.getName()) || excludedFields.contains(propertyDescriptor.getName());
      assertThat(shouldExist).isTrue();
    }

    OverlayInputSetEntity overlayInputSetEntity = OverlayInputSetEntity.builder().build();
    overlayInputSetEntity.setInputSetType(InputSetEntityType.OVERLAY_INPUT_SET);
    updateOperations = InputSetFilterHelper.getUpdateOperations(overlayInputSetEntity);
    stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    propertyDescriptors = BeanUtils.getPropertyDescriptors(OverlayInputSetEntity.class);
    excludedFields = new HashSet<>(Arrays.asList(BaseInputSetEntityKeys.id, BaseInputSetEntityKeys.createdAt,
        BaseInputSetEntityKeys.lastModifiedAt, BaseInputSetEntityKeys.version, "class"));

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
    Update updateOperations = InputSetFilterHelper.getUpdateOperationsForDelete();
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
