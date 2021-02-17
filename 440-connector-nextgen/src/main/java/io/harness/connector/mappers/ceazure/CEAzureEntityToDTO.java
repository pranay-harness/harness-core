package io.harness.connector.mappers.ceazure;

import io.harness.connector.entities.embedded.ceazure.BillingExportDetails;
import io.harness.connector.entities.embedded.ceazure.CEAzureConfig;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO.CEAzureConnectorDTOBuilder;
import io.harness.delegate.beans.connector.ceazure.CEAzureFeatures;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;

@Singleton
public class CEAzureEntityToDTO implements ConnectorEntityToDTOMapper<CEAzureConnectorDTO, CEAzureConfig> {
  @Override
  public CEAzureConnectorDTO createConnectorDTO(CEAzureConfig connector) {
    CEAzureConnectorDTOBuilder builder = CEAzureConnectorDTO.builder()
                                             .featuresEnabled(connector.getFeaturesEnabled())
                                             .subscriptionId(connector.getSubscriptionId())
                                             .tenantId(connector.getTenantId());

    if (connector.getFeaturesEnabled().contains(CEAzureFeatures.BILLING)) {
      final BillingExportDetails billingExportDetails = Preconditions.checkNotNull(connector.getBillingExportDetails(),
          "unexpected, if BILLING is enabled then BillingExportDetails can't be null");
      builder.billingExportSpec(BillingExportSpecDTO.builder()
                                    .containerName(billingExportDetails.getContainerName())
                                    .directoryName(billingExportDetails.getDirectoryName())
                                    .storageAccountName(billingExportDetails.getStorageAccountName())
                                    .build());
    }

    return builder.build();
  }
}
