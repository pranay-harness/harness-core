package io.harness.connector.mappers.ceazure;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.ceazure.BillingExportDetails;
import io.harness.connector.entities.embedded.ceazure.CEAzureConfig;
import io.harness.connector.entities.embedded.ceazure.CEAzureConfig.CEAzureConfigBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureFeatures;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(DX)
public class CEAzureDTOToEntity implements ConnectorDTOToEntityMapper<CEAzureConnectorDTO, CEAzureConfig> {
  @Override
  public CEAzureConfig toConnectorEntity(CEAzureConnectorDTO connectorDTO) {
    final List<CEAzureFeatures> featuresEnabled = connectorDTO.getFeaturesEnabled();

    CEAzureConfigBuilder builder = CEAzureConfig.builder()
                                       .subscriptionId(connectorDTO.getSubscriptionId())
                                       .tenantId(connectorDTO.getTenantId())
                                       .featuresEnabled(featuresEnabled);

    if (featuresEnabled.contains(CEAzureFeatures.BILLING)) {
      final BillingExportSpecDTO billingExportSpecDTO = connectorDTO.getBillingExportSpec();

      if (billingExportSpecDTO == null) {
        throw new InvalidRequestException(
            String.format("billingExportSpec should be provided when the features %s is enabled.",
                CEAzureFeatures.BILLING.getDescription()));
      }

      final BillingExportDetails billingExportDetails =
          BillingExportDetails.builder()
              .storageAccountName(billingExportSpecDTO.getStorageAccountName())
              .containerName(billingExportSpecDTO.getContainerName())
              .directoryName(billingExportSpecDTO.getDirectoryName())
              .build();
      builder.billingExportDetails(billingExportDetails);
    }

    return builder.build();
  }
}
