package io.harness.connector.mappers.gcpcloudcost;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gcpccm.GcpBillingExportDetails;
import io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.gcpccm.GcpBillingExportSpecDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO.GcpCloudCostConnectorDTOBuilder;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostFeatures;

import java.util.List;

@OwnedBy(CE)
public class GcpCloudCostEntityToDTO
    implements ConnectorEntityToDTOMapper<GcpCloudCostConnectorDTO, GcpCloudCostConfig> {
  @Override
  public GcpCloudCostConnectorDTO createConnectorDTO(GcpCloudCostConfig connector) {
    final List<GcpCloudCostFeatures> featuresEnabled = connector.getFeaturesEnabled();

    final GcpCloudCostConnectorDTOBuilder connectorDTOBuilder =
        GcpCloudCostConnectorDTO.builder().featuresEnabled(featuresEnabled);

    if (featuresEnabled.contains(GcpCloudCostFeatures.BILLING)) {
      // in DTOtoEntity we are making sure that if BILLING is enabled then the billingExportSpec shouldn't be null
      populateBillingAttributes(connectorDTOBuilder, connector.getBillingExportDetails());
    }

    return connectorDTOBuilder.build();
  }

  private void populateBillingAttributes(
      final GcpCloudCostConnectorDTOBuilder connectorDTOBuilder, final GcpBillingExportDetails billingExportDetails) {
    connectorDTOBuilder.billingExportSpec(GcpBillingExportSpecDTO.builder()
                                              .projectId(billingExportDetails.getProjectId())
                                              .datasetId(billingExportDetails.getDatasetId())
                                              .build());
  }
}
