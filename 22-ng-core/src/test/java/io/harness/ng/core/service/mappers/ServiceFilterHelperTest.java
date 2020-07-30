package io.harness.ng.core.service.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.entity.ServiceEntity.ServiceEntityKeys;
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

public class ServiceFilterHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetCriteria() {
    String accountId = "ACCOUNT_ID";
    String orgIdentifier = "ORG_ID";
    String projectIdentifier = "PROJECT_ID";
    Criteria criteriaFromServiceFilter =
        ServiceFilterHelper.createCriteria(accountId, orgIdentifier, projectIdentifier);
    assertThat(criteriaFromServiceFilter).isNotNull();
    Document criteriaObject = criteriaFromServiceFilter.getCriteriaObject();
    assertThat(criteriaObject.get(ServiceEntityKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(ServiceEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(ServiceEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    ServiceEntity serviceEntity = ServiceEntity.builder().build();
    Update updateOperations = ServiceFilterHelper.getUpdateOperations(serviceEntity);
    Set<String> stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(ServiceEntity.class);
    Set<String> excludedFields = new HashSet<>(Arrays.asList(ServiceEntityKeys.id, ServiceEntityKeys.createdAt,
        ServiceEntityKeys.lastModifiedAt, ServiceEntityKeys.version, "class"));

    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      boolean shouldExist =
          stringSet.contains(propertyDescriptor.getName()) || excludedFields.contains(propertyDescriptor.getName());
      assertThat(shouldExist).isTrue();
    }
  }
}