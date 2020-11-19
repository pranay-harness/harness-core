package io.harness.batch.processing.service;

import com.google.inject.Singleton;

import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.service.intfc.AccountExpiryService;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import io.harness.ccm.license.CeLicenseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEGcpConfig;
import software.wings.beans.ce.CEMetadataRecord;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@Service
public class AccountExpiryCleanupService {
  @Autowired private AccountExpiryService accountExpiryService;
  @Autowired private BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;

  public void execute() {
    List<Account> accounts = cloudToHarnessMappingService.getCeAccountsWithLicense();
    log.info("Accounts batch size is AccountExpiryCleanupTasklet {} ", accounts.size());
    accounts.forEach(account -> {
      Boolean isAwsConnectorPresent = false;
      Boolean isGCPConnectorPresent = false;

      String accountId = account.getUuid();
      List<SettingAttribute> ceConnectors = cloudToHarnessMappingService.getCEConnectors(accountId);
      Set<String> settingIdsSet =
          ceConnectors.stream()
              .filter(connector -> connector.getValue().getType().equals(SettingVariableTypes.CE_AWS.toString()))
              .map(SettingAttribute::getUuid)
              .collect(Collectors.toSet());

      if (!settingIdsSet.isEmpty()) {
        isAwsConnectorPresent = true;
      }

      for (SettingAttribute ceConnector : ceConnectors) {
        SettingValue value = ceConnector.getValue();
        if (value.getType().equals(SettingVariableTypes.CE_GCP.toString())) {
          isGCPConnectorPresent = true;
          settingIdsSet.add(((CEGcpConfig) value).getOrganizationSettingId());
        }
      }

      cloudToHarnessMappingService.upsertCEMetaDataRecord(CEMetadataRecord.builder()
                                                              .accountId(accountId)
                                                              .awsConnectorConfigured(isAwsConnectorPresent)
                                                              .gcpConnectorConfigured(isGCPConnectorPresent)
                                                              .build());

      List<BillingDataPipelineRecord> billingDataPipelineRecordList =
          billingDataPipelineRecordDao.getAllRecordsByAccountId(accountId);

      for (BillingDataPipelineRecord record : billingDataPipelineRecordList) {
        if (!settingIdsSet.contains(record.getSettingId())) {
          accountExpiryService.deletePipelinePerRecord(accountId, record);
        }
      }

      CeLicenseInfo ceLicenseInfo = account.getCeLicenseInfo();
      long expiryTime = ceLicenseInfo.getExpiryTimeWithGracePeriod();
      if (expiryTime != 0L && Instant.now().toEpochMilli() > expiryTime) {
        log.info("Triggering Data Pipeline Clean up for account: {} ", account);
        accountExpiryService.dataPipelineCleanup(account);
      }
    });
  }
}
