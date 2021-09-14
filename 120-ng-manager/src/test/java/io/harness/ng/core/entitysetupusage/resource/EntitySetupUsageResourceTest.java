/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entitysetupusage.resource;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(DX)
public class EntitySetupUsageResourceTest extends CategoryTest {
  @InjectMocks EntitySetupUsageResource entitySetupUsageResource;
  @Mock EntitySetupUsageService entitySetupUsageService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void listTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    String searchTerm = "searchTerm";
    String referredEntityFQN = "referredEntityFQN";
    entitySetupUsageResource.listAllEntityUsage(
        100, 100, accountIdentifier, referredEntityFQN, EntityType.CONNECTORS, searchTerm);
    Mockito.verify(entitySetupUsageService, times(1))
        .listAllEntityUsage(
            eq(100), eq(100), eq(accountIdentifier), eq(referredEntityFQN), eq(EntityType.CONNECTORS), eq(searchTerm));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void isEntityReferenced() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    String referredEntityFQN = "referredEntityFQN";
    entitySetupUsageResource.isEntityReferenced(accountIdentifier, referredEntityFQN, EntityType.CONNECTORS);
    Mockito.verify(entitySetupUsageService, times(1))
        .isEntityReferenced(eq(accountIdentifier), eq(referredEntityFQN), eq(EntityType.CONNECTORS));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void saveTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    EntitySetupUsageDTO entitySetupUsageDTO =
        EntitySetupUsageDTO.builder().accountIdentifier(accountIdentifier).build();
    entitySetupUsageResource.save(entitySetupUsageDTO);
    Mockito.verify(entitySetupUsageService, times(1)).save(eq(entitySetupUsageDTO));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void deleteTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    String referredEntityFQN = "referredEntityFQN";
    String referredByEntityFQN = "referredByEntityFQN";
    entitySetupUsageResource.delete(
        accountIdentifier, referredEntityFQN, EntityType.CONNECTORS, referredEntityFQN, EntityType.SECRETS);
    Mockito.verify(entitySetupUsageService, times(1))
        .delete(eq(accountIdentifier), eq(referredEntityFQN), eq(EntityType.CONNECTORS), eq(referredEntityFQN),
            eq(EntityType.SECRETS));
  }
}
