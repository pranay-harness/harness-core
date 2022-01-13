/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.environment.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;
import io.harness.ng.core.utils.CoreCriteriaUtils;
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

@OwnedBy(HarnessTeam.CDC)
public class EnvironmentFilterHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetCriteria() {
    String accountId = "ACCOUNT_ID";
    String orgIdentifier = "ORG_ID";
    String projectIdentifier = "PROJECT_ID";
    Criteria criteriaFromServiceFilter =
        CoreCriteriaUtils.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier, false);
    assertThat(criteriaFromServiceFilter).isNotNull();
    Document criteriaObject = criteriaFromServiceFilter.getCriteriaObject();
    assertThat(criteriaObject.get(EnvironmentKeys.accountId)).isEqualTo(accountId);
    assertThat(criteriaObject.get(EnvironmentKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(EnvironmentKeys.projectIdentifier)).isEqualTo(projectIdentifier);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    Environment environment = Environment.builder().build();
    Update updateOperations = EnvironmentFilterHelper.getUpdateOperations(environment);
    Set<String> stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(Environment.class);
    Set<String> excludedFields =
        new HashSet<>(Arrays.asList(EnvironmentKeys.id, EnvironmentKeys.createdAt, EnvironmentKeys.version, "class"));

    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      boolean shouldExist =
          stringSet.contains(propertyDescriptor.getName()) || excludedFields.contains(propertyDescriptor.getName());
      assertThat(shouldExist).isTrue();
    }

    Set<String> setOnInsert = ((Document) updateOperations.getUpdateObject().get("$setOnInsert")).keySet();
    assertThat(setOnInsert).hasSize(1);
    assertThat(setOnInsert).contains(EnvironmentKeys.createdAt);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetUpdateOperationsForDelete() {
    Update updateOperations = EnvironmentFilterHelper.getUpdateOperationsForDelete();
    Set<String> stringSet = ((Document) updateOperations.getUpdateObject().get("$set")).keySet();
    PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(Environment.class);

    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      if (propertyDescriptor.getName().equals("deleted")) {
        assertThat(stringSet.contains(propertyDescriptor.getName())).isTrue();
      } else {
        assertThat(stringSet.contains(propertyDescriptor.getName())).isFalse();
      }
    }
  }
}
