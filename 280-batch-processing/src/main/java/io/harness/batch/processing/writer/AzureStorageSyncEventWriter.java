package io.harness.batch.processing.writer;

import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;
import static software.wings.settings.SettingVariableTypes.CE_AZURE;

import io.harness.batch.processing.ccm.AzureStorageSyncRecord;
import io.harness.batch.processing.ccm.AzureStorageSyncRecord.AzureStorageSyncRecordBuilder;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.service.impl.AzureStorageSyncServiceImpl;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAzureConfig;

import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class AzureStorageSyncEventWriter extends EventWriter implements ItemWriter<SettingAttribute> {
  @Autowired private AzureStorageSyncServiceImpl azureStorageSyncService;
  private JobParameters parameters;

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
  }

  @Override
  public void write(List<? extends SettingAttribute> dummySettingAttributeList) {
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);

    List<SettingAttribute> ceConnectorsList =
        cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(accountId, CE_CONNECTOR, CE_AZURE);

    log.info("Processing batch size of {} in AzureStorageSyncEventWriter", ceConnectorsList.size());

    ceConnectorsList.forEach(settingAttribute -> {
      AzureStorageSyncRecordBuilder azureStorageSyncRecordBuilder = AzureStorageSyncRecord.builder();
      azureStorageSyncRecordBuilder.accountId(settingAttribute.getAccountId());
      azureStorageSyncRecordBuilder.settingId(settingAttribute.getUuid());
      azureStorageSyncRecordBuilder.containerName(((CEAzureConfig) settingAttribute.getValue()).getContainerName());
      azureStorageSyncRecordBuilder.directoryName(((CEAzureConfig) settingAttribute.getValue()).getDirectoryName());
      azureStorageSyncRecordBuilder.subscriptionId(((CEAzureConfig) settingAttribute.getValue()).getSubscriptionId());
      azureStorageSyncRecordBuilder.storageAccountName(
          ((CEAzureConfig) settingAttribute.getValue()).getStorageAccountName());
      azureStorageSyncRecordBuilder.tenantId(((CEAzureConfig) settingAttribute.getValue()).getTenantId());

      if (settingAttribute.getValue() instanceof CEAzureConfig) {
        azureStorageSyncService.syncContainer(azureStorageSyncRecordBuilder.build());
      }
    });
  }
}
