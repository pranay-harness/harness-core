package io.harness.connector.mappers.ceazure;

import io.harness.connector.entities.embedded.ceazure.BillingExportDetails;
import io.harness.connector.entities.embedded.ceazure.CEAzureConfig;
import io.harness.connector.entities.embedded.ceazure.CEAzureConfig.CEAzureConfigBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureFeatures;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGAccess;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CEAzureDTOToEntity implements ConnectorDTOToEntityMapper<CEAzureConnectorDTO, CEAzureConfig> {
  @Override
  public CEAzureConfig toConnectorEntity(CEAzureConnectorDTO connectorDTO, NGAccess ngAccess) {
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
