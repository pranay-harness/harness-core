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
import io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig.GcpCloudCostConfigBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.gcpccm.GcpBillingExportSpecDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.exception.InvalidRequestException;

import java.util.List;

@OwnedBy(CE)
public class GcpCloudCostDTOToEntity
    implements ConnectorDTOToEntityMapper<GcpCloudCostConnectorDTO, GcpCloudCostConfig> {
  @Override
  public GcpCloudCostConfig toConnectorEntity(GcpCloudCostConnectorDTO connectorDTO) {
    final List<CEFeatures> featuresEnabled = connectorDTO.getFeaturesEnabled();

    final GcpCloudCostConfigBuilder configBuilder = GcpCloudCostConfig.builder()
                                                        .featuresEnabled(featuresEnabled)
                                                        .serviceAccountEmail(connectorDTO.getServiceAccountEmail())
                                                        .projectId(connectorDTO.getProjectId());

    if (featuresEnabled.contains(CEFeatures.BILLING)) {
      populateBillingAttributes(configBuilder, connectorDTO.getBillingExportSpec());
    }

    return configBuilder.build();
  }

  private void populateBillingAttributes(
      final GcpCloudCostConfigBuilder configBuilder, final GcpBillingExportSpecDTO billingExportSpecDTO) {
    if (billingExportSpecDTO == null) {
      throw new InvalidRequestException(
          String.format("billingAttributes should be provided when the features %s is enabled.",
              CEFeatures.BILLING.getDescription()));
    }

    configBuilder.billingExportDetails(GcpBillingExportDetails.builder()
                                           .datasetId(billingExportSpecDTO.getDatasetId())
                                           .tableId(billingExportSpecDTO.getTableId())
                                           .build());
  }
}
