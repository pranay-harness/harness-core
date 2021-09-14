/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
