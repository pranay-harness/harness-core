package io.harness.batch.processing.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.ccm.commons.dao.CEMetadataRecordDao;
import io.harness.ccm.commons.entities.CEMetadataRecord;

import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomBillingMetaDataServiceImpl implements CustomBillingMetaDataService {
  private final CloudToHarnessMappingService cloudToHarnessMappingService;
  private final BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  private final BigQueryHelperService bigQueryHelperService;
  private final CEMetadataRecordDao ceMetadataRecordDao;
  private final CloudBillingHelper cloudBillingHelper;

  private LoadingCache<String, String> awsBillingMetaDataCache =
      Caffeine.newBuilder().expireAfterWrite(4, TimeUnit.HOURS).build(this::getAwsBillingMetaData);

  private LoadingCache<String, String> azureBillingMetaDataCache =
          Caffeine.newBuilder().expireAfterWrite(4, TimeUnit.HOURS).build(this::getAzureBillingMetaData);

  private LoadingCache<CacheKey, Boolean> pipelineJobStatusCache =
      Caffeine.newBuilder()
          .expireAfterWrite(4, TimeUnit.HOURS)
          .build(key -> getPipelineJobStatus(key.accountId, key.startTime, key.endTime));

  @Value
  @AllArgsConstructor
  private static class CacheKey {
    private String accountId;
    private Instant startTime;
    private Instant endTime;
  }
  @Autowired
  public CustomBillingMetaDataServiceImpl(CloudToHarnessMappingService cloudToHarnessMappingService,
      BillingDataPipelineRecordDao billingDataPipelineRecordDao, BigQueryHelperService bigQueryHelperService,
      CEMetadataRecordDao ceMetadataRecordDao, CloudBillingHelper cloudBillingHelper) {
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
    this.billingDataPipelineRecordDao = billingDataPipelineRecordDao;
    this.bigQueryHelperService = bigQueryHelperService;
    this.ceMetadataRecordDao = ceMetadataRecordDao;
    this.cloudBillingHelper = cloudBillingHelper;
  }

  @Override
  public String getAwsDataSetId(String accountId) {
    return awsBillingMetaDataCache.get(accountId);
  }

  @Override
  public String getAzureDataSetId(String accountId) {
    return azureBillingMetaDataCache.get(accountId);
  }

  @Override
  public Boolean checkPipelineJobFinished(String accountId, Instant startTime, Instant endTime) {
    CacheKey cacheKey = new CacheKey(accountId, startTime, endTime);
    return pipelineJobStatusCache.get(cacheKey);
  }

  private Boolean getPipelineJobStatus(String accountId, Instant startTime, Instant endTime) {
    String awsDataSetId = getAwsDataSetId(accountId);
    // For 4 days before date always return true; If CUR data is not present it will use public api
    Instant bufferTime = Instant.now().minus(4, ChronoUnit.DAYS);
    if (awsDataSetId != null && startTime.isAfter(bufferTime)) {
      Instant startAt = endTime.minus(1, ChronoUnit.HOURS);
      Map<String, VMInstanceBillingData> awsEC2BillingData =
          bigQueryHelperService.getAwsBillingData(startAt, endTime, awsDataSetId);
      return isNotEmpty(awsEC2BillingData);
    }
    return Boolean.TRUE;
  }

  private String getAwsBillingMetaData(String accountId) {
    CEMetadataRecord ceMetadataRecord = ceMetadataRecordDao.getByAccountId(accountId);
    if (null != ceMetadataRecord && null != ceMetadataRecord.getAwsDataPresent()
        && ceMetadataRecord.getAwsDataPresent()) {
      return cloudBillingHelper.getDataSetId(accountId);
    }
    return null;
  }

  private String getAzureBillingMetaData(String accountId) {
    // Enabled for all
    List<SettingAttribute> settingAttributes = cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(
            accountId, SettingAttribute.SettingCategory.CE_CONNECTOR, SettingVariableTypes.CE_AZURE);
    log.info("Setting att size {}", settingAttributes.size());
    if (!settingAttributes.isEmpty()) {
      SettingAttribute settingAttribute = settingAttributes.get(0);
      BillingDataPipelineRecord billingDataPipelineRecord =
              billingDataPipelineRecordDao.getBySettingId(accountId, settingAttribute.getUuid());
      log.info("BDPR {}", billingDataPipelineRecord);
      return billingDataPipelineRecord.getDataSetId();
    }
    return null;
  }
}
