/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static junit.framework.TestCase.assertNotNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDC)
public class TemplateServiceConfigurationTest extends CategoryTest {
  @InjectMocks TemplateServiceConfiguration configuration;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration swaggerBundleConfiguration = configuration.getSwaggerBundleConfiguration();
    assertNotNull(swaggerBundleConfiguration);
    assertNotNull(swaggerBundleConfiguration.getResourcePackage());
    assertNotNull(swaggerBundleConfiguration.getTitle());
    assertNotNull(swaggerBundleConfiguration.getVersion());
    assertNotNull(swaggerBundleConfiguration.getSchemes());
  }
}
