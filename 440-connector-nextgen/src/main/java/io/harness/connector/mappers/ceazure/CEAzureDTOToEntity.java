/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.ceazure;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.ceazure.BillingExportDetails;
import io.harness.connector.entities.embedded.ceazure.CEAzureConfig;
import io.harness.connector.entities.embedded.ceazure.CEAzureConfig.CEAzureConfigBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(CE)
public class CEAzureDTOToEntity implements ConnectorDTOToEntityMapper<CEAzureConnectorDTO, CEAzureConfig> {
  @Override
  public CEAzureConfig toConnectorEntity(CEAzureConnectorDTO connectorDTO) {
    final List<CEFeatures> featuresEnabled = connectorDTO.getFeaturesEnabled();

    final CEAzureConfigBuilder configBuilder = CEAzureConfig.builder()
                                                   .subscriptionId(connectorDTO.getSubscriptionId())
                                                   .tenantId(connectorDTO.getTenantId())
                                                   .featuresEnabled(featuresEnabled);

    if (featuresEnabled.contains(CEFeatures.BILLING)) {
      populateBillingExportDetails(configBuilder, connectorDTO.getBillingExportSpec());
    }

    return configBuilder.build();
  }

  private void populateBillingExportDetails(
      CEAzureConfigBuilder configBuilder, final BillingExportSpecDTO billingExportSpecDTO) {
    if (billingExportSpecDTO == null) {
      throw new InvalidRequestException(
          String.format("billingExportSpec should be provided when the features %s is enabled.",
              CEFeatures.BILLING.getDescription()));
    }

    final BillingExportDetails billingExportDetails =
        BillingExportDetails.builder()
            .storageAccountName(billingExportSpecDTO.getStorageAccountName())
            .containerName(billingExportSpecDTO.getContainerName())
            .directoryName(billingExportSpecDTO.getDirectoryName())
            .reportName(billingExportSpecDTO.getReportName())
            .subscriptionId(billingExportSpecDTO.getSubscriptionId())
            .build();
    configBuilder.billingExportDetails(billingExportDetails);
  }
}
