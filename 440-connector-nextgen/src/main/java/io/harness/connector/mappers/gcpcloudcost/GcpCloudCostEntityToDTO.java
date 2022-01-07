/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.gcpcloudcost;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gcpccm.GcpBillingExportDetails;
import io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.gcpccm.GcpBillingExportSpecDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO.GcpCloudCostConnectorDTOBuilder;

import java.util.List;

@OwnedBy(CE)
public class GcpCloudCostEntityToDTO
    implements ConnectorEntityToDTOMapper<GcpCloudCostConnectorDTO, GcpCloudCostConfig> {
  @Override
  public GcpCloudCostConnectorDTO createConnectorDTO(GcpCloudCostConfig connector) {
    final List<CEFeatures> featuresEnabled = connector.getFeaturesEnabled();

    final GcpCloudCostConnectorDTOBuilder connectorDTOBuilder =
        GcpCloudCostConnectorDTO.builder()
            .featuresEnabled(featuresEnabled)
            .serviceAccountEmail(connector.getServiceAccountEmail())
            .projectId(connector.getProjectId());

    if (featuresEnabled.contains(CEFeatures.BILLING)) {
      // in DTOtoEntity we are making sure that if BILLING is enabled then the billingExportSpec shouldn't be null
      populateBillingAttributes(connectorDTOBuilder, connector.getBillingExportDetails());
    }

    return connectorDTOBuilder.build();
  }

  private void populateBillingAttributes(
      final GcpCloudCostConnectorDTOBuilder connectorDTOBuilder, final GcpBillingExportDetails billingExportDetails) {
    connectorDTOBuilder.billingExportSpec(GcpBillingExportSpecDTO.builder()
                                              .datasetId(billingExportDetails.getDatasetId())
                                              .tableId(billingExportDetails.getTableId())
                                              .build());
  }
}
