package io.harness.connector.mappers.gcpcloudcost;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gcpccm.GcpBillingExportDetails;
import io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig;
import io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig.GcpCloudCostConfigBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.gcpccm.GcpBillingExportSpecDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostFeatures;
import io.harness.exception.InvalidRequestException;

import java.util.List;

@OwnedBy(CE)
public class GcpCloudCostDTOToEntity
    implements ConnectorDTOToEntityMapper<GcpCloudCostConnectorDTO, GcpCloudCostConfig> {
  @Override
  public GcpCloudCostConfig toConnectorEntity(GcpCloudCostConnectorDTO connectorDTO) {
    final List<GcpCloudCostFeatures> featuresEnabled = connectorDTO.getFeaturesEnabled();

    final GcpCloudCostConfigBuilder configBuilder = GcpCloudCostConfig.builder().featuresEnabled(featuresEnabled);

    if (featuresEnabled.contains(GcpCloudCostFeatures.BILLING)) {
      populateBillingAttributes(configBuilder, connectorDTO.getBillingExportSpec());
    }

    return configBuilder.build();
  }

  private void populateBillingAttributes(
      final GcpCloudCostConfigBuilder configBuilder, final GcpBillingExportSpecDTO billingExportSpecDTO) {
    if (billingExportSpecDTO == null) {
      throw new InvalidRequestException(
          String.format("billingAttributes should be provided when the features %s is enabled.",
              GcpCloudCostFeatures.BILLING.getDescription()));
    }

    configBuilder.billingExportDetails(GcpBillingExportDetails.builder()
                                           .projectId(billingExportSpecDTO.getProjectId())
                                           .datasetId(billingExportSpecDTO.getDatasetId())
                                           .build());
  }
}
