/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.resources;

import static io.harness.rule.OwnerRule.DEEPAK;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.services.api.CVSetupService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CVSetupResourceTest extends CategoryTest {
  @InjectMocks private CVSetupResource cvSetupResource;
  @Mock CVSetupService cvSetupService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testGetCVSetupStatus() {
    String accountId = "accountId";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    cvSetupResource.getCVSetupStatus(accountId, orgIdentifier, projectIdentifier);
    verify(cvSetupService, times(1)).getSetupStatus(eq(accountId), eq(orgIdentifier), eq(projectIdentifier));
  }
}
