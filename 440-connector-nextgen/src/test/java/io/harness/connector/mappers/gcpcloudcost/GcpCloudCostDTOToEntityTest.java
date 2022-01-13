/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.gcpcloudcost;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig;
import io.harness.connector.utils.GcpConnectorTestHelper;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CE)
public class GcpCloudCostDTOToEntityTest extends CategoryTest {
  @InjectMocks GcpCloudCostDTOToEntity dtoToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity() {
    final GcpCloudCostConfig connectorEntity =
        dtoToEntity.toConnectorEntity(GcpConnectorTestHelper.createGcpCcmConnectorDTO());

    assertThat(connectorEntity).isEqualTo(GcpConnectorTestHelper.createGcpCcmConfig());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testThrowOnBillingAttributesMissing() {
    final GcpCloudCostConnectorDTO connectorDTO = GcpConnectorTestHelper.createGcpCcmConnectorDTO();
    connectorDTO.setBillingExportSpec(null);

    assertThatThrownBy(() -> dtoToEntity.toConnectorEntity(connectorDTO))
        .isExactlyInstanceOf(InvalidRequestException.class);
  }
}
