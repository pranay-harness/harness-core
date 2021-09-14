/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.ceazure;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.utils.AzureConnectorTestHelper;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CEAzureEntityToDTOTest extends CategoryTest {
  @InjectMocks CEAzureEntityToDTO ceAzureEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testCreateConnectorDTOAll() {
    final CEAzureConnectorDTO ceAzureConnectorDTO =
        ceAzureEntityToDTO.createConnectorDTO(AzureConnectorTestHelper.createCEAzureConfig());
    assertThat(ceAzureConnectorDTO).isEqualTo(AzureConnectorTestHelper.createCEAzureConnectorDTO());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testBillingOnly() {
    final CEAzureConnectorDTO ceAzureConnectorDTO =
        ceAzureEntityToDTO.createConnectorDTO(AzureConnectorTestHelper.createCEAzureConfigBillingOnly());
    assertThat(ceAzureConnectorDTO).isEqualTo(AzureConnectorTestHelper.createCEAzureConnectorDTOBillingOnly());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testOptimizationOnly() {
    final CEAzureConnectorDTO ceAzureConnectorDTO =
        ceAzureEntityToDTO.createConnectorDTO(AzureConnectorTestHelper.createCEAzureConfigOptimizationOnly());
    assertThat(ceAzureConnectorDTO).isEqualTo(AzureConnectorTestHelper.createCEAzureConnectorDTOOptimizationOnly());
  }
}
