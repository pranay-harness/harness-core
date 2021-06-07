package io.harness.batch.processing.view;

import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.commons.entities.batch.CEMetadataRecord;
import io.harness.ccm.commons.entities.batch.CEMetadataRecord.CEMetadataRecordBuilder;
import io.harness.ccm.views.service.CEViewService;

import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Singleton;
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
  @Autowired private BigQueryHelperService bigQueryHelperService;
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
}
