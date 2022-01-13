/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.entitysetupusage;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(DX)
public class EntitySetupUsageFilterHelperTest extends CategoryTest {
  @InjectMocks EntitySetupUsageQueryFilterHelper entitySetupUsageFilterHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void createCriteriaFromEntityFilter() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String referredEntityFQN = "referredEntityFQN";
    String searchTerm = "searchTerm";

    Criteria criteria = entitySetupUsageFilterHelper.createCriteriaFromEntityFilter(
        accountIdentifier, referredEntityFQN, EntityType.CONNECTORS, searchTerm);
    assertThat(criteria.getCriteriaObject().size()).isEqualTo(5);
    assertThat(criteria.getCriteriaObject().get(EntitySetupUsageKeys.referredEntityFQN)).isEqualTo(referredEntityFQN);
  }
}
