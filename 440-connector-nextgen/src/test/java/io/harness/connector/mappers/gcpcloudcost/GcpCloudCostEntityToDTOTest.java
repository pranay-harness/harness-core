/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.gcpcloudcost;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.utils.GcpConnectorTestHelper;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CE)
public class GcpCloudCostEntityToDTOTest extends CategoryTest {
  @InjectMocks GcpCloudCostEntityToDTO entityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testCreateConnectorDTO() {
    final GcpCloudCostConnectorDTO gcpCcmConnectorDTO =
        entityToDTO.createConnectorDTO(GcpConnectorTestHelper.createGcpCcmConfig());

    assertThat(gcpCcmConnectorDTO).isEqualTo(GcpConnectorTestHelper.createGcpCcmConnectorDTO());
  }
}
