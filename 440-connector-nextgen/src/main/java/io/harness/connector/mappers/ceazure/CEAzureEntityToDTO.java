/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.ceazure;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.ceazure.BillingExportDetails;
import io.harness.connector.entities.embedded.ceazure.CEAzureConfig;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO.CEAzureConnectorDTOBuilder;

import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;

@Singleton
@OwnedBy(CE)
public class CEAzureEntityToDTO implements ConnectorEntityToDTOMapper<CEAzureConnectorDTO, CEAzureConfig> {
  @Override
  public CEAzureConnectorDTO createConnectorDTO(CEAzureConfig connector) {
    CEAzureConnectorDTOBuilder connectorDTOBuilder = CEAzureConnectorDTO.builder()
                                                         .featuresEnabled(connector.getFeaturesEnabled())
                                                         .subscriptionId(connector.getSubscriptionId())
                                                         .tenantId(connector.getTenantId());

    if (connector.getFeaturesEnabled().contains(CEFeatures.BILLING)) {
      populateBillingExportDetails(connectorDTOBuilder, connector.getBillingExportDetails());
    }

    return connectorDTOBuilder.build();
  }

  private void populateBillingExportDetails(@NotNull final CEAzureConnectorDTOBuilder connectorDTOBuilder,
      @NotNull final BillingExportDetails billingExportDetails) {
    connectorDTOBuilder.billingExportSpec(BillingExportSpecDTO.builder()
                                              .containerName(billingExportDetails.getContainerName())
                                              .directoryName(billingExportDetails.getDirectoryName())
                                              .storageAccountName(billingExportDetails.getStorageAccountName())
                                              .reportName(billingExportDetails.getReportName())
                                              .subscriptionId(billingExportDetails.getSubscriptionId())
                                              .build());
  }
}
