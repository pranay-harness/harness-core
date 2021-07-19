package io.harness.batch.processing.view;

import static io.harness.beans.FeatureName.CE_AWS_BILLING_CONNECTOR_DETAIL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.commons.entities.batch.CEMetadataRecord;
import io.harness.ccm.commons.entities.batch.CEMetadataRecord.CEMetadataRecordBuilder;
import io.harness.ccm.views.service.CEViewService;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.ff.FeatureFlagService;
import io.harness.filter.FilterType;
import io.harness.ng.beans.PageResponse;

import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class CEMetaDataRecordUpdateService {
  @Autowired private AccountShardService accountShardService;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private ConnectorResourceClient connectorResourceClient;
  @Autowired private BigQueryHelperService bigQueryHelperService;
  @Autowired private FeatureFlagService featureFlagService;
  @Autowired private CEViewService ceViewService;

  public void updateCloudProviderMetadata() {
    List<Account> ceEnabledAccounts = accountShardService.getCeEnabledAccounts();
    List<String> accountIds = ceEnabledAccounts.stream().map(Account::getUuid).collect(Collectors.toList());
    accountIds.forEach(this::updateCloudProviderMetadata);
  }

  private void updateCloudProviderMetadata(String accountId) {
    try {
      List<SettingAttribute> ceConnectors = cloudToHarnessMappingService.getCEConnectors(accountId);
      boolean isAwsConnectorPresent = ceConnectors.stream().anyMatch(
          connector -> connector.getValue().getType().equals(SettingVariableTypes.CE_AWS.toString()));

      boolean isGCPConnectorPresent = ceConnectors.stream().anyMatch(
          connector -> connector.getValue().getType().equals(SettingVariableTypes.CE_GCP.toString()));

      boolean isAzureConnectorPresent = ceConnectors.stream().anyMatch(
          connector -> connector.getValue().getType().equals(SettingVariableTypes.CE_AZURE.toString()));

      if (featureFlagService.isEnabled(CE_AWS_BILLING_CONNECTOR_DETAIL, accountId)) {
        List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
        PageResponse<ConnectorResponseDTO> response = null;
        int page = 0;
        int size = 100;
        ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
            ConnectorFilterPropertiesDTO.builder()
                .ccmConnectorFilter(
                    CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.BILLING)).build())
                .build();
        connectorFilterPropertiesDTO.setFilterType(FilterType.CONNECTOR);
        do {
          response = execute(connectorResourceClient.listConnectors(
              accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
          if (response != null && isNotEmpty(response.getContent())) {
            nextGenConnectorResponses.addAll(response.getContent());
          }
          page++;
        } while (response != null && isNotEmpty(response.getContent()));

        isAwsConnectorPresent =
            updateConnectorPresent(isAwsConnectorPresent, ConnectorType.CE_AWS, nextGenConnectorResponses);
        isGCPConnectorPresent =
            updateConnectorPresent(isGCPConnectorPresent, ConnectorType.GCP_CLOUD_COST, nextGenConnectorResponses);
        isAzureConnectorPresent =
            updateConnectorPresent(isAzureConnectorPresent, ConnectorType.CE_AZURE, nextGenConnectorResponses);
      }

      if (isAzureConnectorPresent && ceViewService.getDefaultAzureViewId(accountId) == null) {
        ceViewService.createDefaultAzureView(accountId);
      }

      CEMetadataRecordBuilder ceMetadataRecordBuilder = CEMetadataRecord.builder().accountId(accountId);

      if (isAwsConnectorPresent || isGCPConnectorPresent || isAzureConnectorPresent) {
        bigQueryHelperService.updateCloudProviderMetaData(accountId, ceMetadataRecordBuilder);
      }

      cloudToHarnessMappingService.upsertCEMetaDataRecord(
          ceMetadataRecordBuilder.awsConnectorConfigured(isAwsConnectorPresent)
              .gcpConnectorConfigured(isGCPConnectorPresent)
              .azureConnectorConfigured(isAzureConnectorPresent)
              .build());
    } catch (Exception ex) {
      log.error("Exception while updateCloudProviderMetadata", ex);
    }
  }

  private boolean updateConnectorPresent(
      boolean connectorPresent, ConnectorType connectorType, List<ConnectorResponseDTO> nextGenConnectorResponses) {
    if (!connectorPresent) {
      connectorPresent = nextGenConnectorResponses.stream().anyMatch(
          connectorResponseDTO -> connectorResponseDTO.getConnector().getConnectorType().equals(connectorType));
    }
    return connectorPresent;
  }
}
