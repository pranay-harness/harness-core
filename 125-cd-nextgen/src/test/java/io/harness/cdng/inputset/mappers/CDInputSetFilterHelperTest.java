package io.harness.cdng.inputset.mappers;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity.CDInputSetEntityKeys;
import io.harness.rule.Owner;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CDInputSetFilterHelperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetCriteria() {
    String accountId = "ACCOUNT_ID";
    String orgIdentifier = "ORG_ID";
    String projectIdentifier = "PROJECT_ID";
    String pipelineIdentifier = "PIPELINE_ID";
    Criteria criteriaFromFilter = CDInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    assertThat(criteriaFromFilter).isNotNull();

    Document criteriaObject = criteriaFromFilter.getCriteriaObject();
    assertThat(criteriaObject.get(CDInputSetEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(CDInputSetEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(CDInputSetEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(CDInputSetEntityKeys.pipelineIdentifier)).isEqualTo(pipelineIdentifier);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    CDInputSetEntity cdInputSetEntity = CDInputSetEntity.builder().build();
    Update updateOperations = CDInputSetFilterHelper.getUpdateOperations(cdInputSetEntity);
    Set<String> stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(CDInputSetEntity.class);
    Set<String> excludedFields = new HashSet<>(Arrays.asList(CDInputSetEntityKeys.id, CDInputSetEntityKeys.createdAt,
        CDInputSetEntityKeys.lastModifiedAt, CDInputSetEntityKeys.version, "class"));

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
    Update updateOperations = CDInputSetFilterHelper.getUpdateOperationsForDelete();
    Set<String> stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(CDInputSetEntity.class);

    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      if (propertyDescriptor.getName().equals("deleted")) {
        assertThat(stringSet.contains(propertyDescriptor.getName())).isTrue();
      } else {
        assertThat(stringSet.contains(propertyDescriptor.getName())).isFalse();
      }
    }
  }
}