/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.activityhistory;

import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.activityhistory.entity.NGActivity.ActivityHistoryEntityKeys;
import io.harness.rule.Owner;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Singleton;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
public class NGActivityQueryCriteriaHelperTest extends CategoryTest {
  @InjectMocks NGActivityQueryCriteriaHelper ngActivityQueryCriteriaHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void addTimeFilterInTheCriteriaTest() {
    long startTime = 10;
    long endTime = 100;
    Criteria criteria = new Criteria();
    ngActivityQueryCriteriaHelper.addTimeFilterInTheCriteria(criteria, startTime, endTime);
    Document criteriaToTest = (Document) criteria.getCriteriaObject().get(ActivityHistoryEntityKeys.activityTime);
    assertThat(criteriaToTest.get("$gte")).isEqualTo(10L);
    assertThat(criteriaToTest.get("$lt")).isEqualTo(100L);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void populateEntityFQNFilterInCriteriaTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String referredEntityIdentifier = "referredEntityIdentifier";
    Criteria criteria = new Criteria();
    ngActivityQueryCriteriaHelper.populateEntityFQNFilterInCriteria(
        criteria, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier);
    assertThat(criteria.getCriteriaObject().get(ActivityHistoryEntityKeys.referredEntityFQN))
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier));
  }
}
