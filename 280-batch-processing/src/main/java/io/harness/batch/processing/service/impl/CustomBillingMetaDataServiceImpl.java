package io.harness.batch.processing.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingVariableTypes;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

  private LoadingCache<String, String> awsBillingMetaDataCache =
      Caffeine.newBuilder().expireAfterWrite(4, TimeUnit.HOURS).build(this::getAwsBillingMetaData);

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
      BillingDataPipelineRecordDao billingDataPipelineRecordDao, BigQueryHelperService bigQueryHelperService) {
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
    this.billingDataPipelineRecordDao = billingDataPipelineRecordDao;
    this.bigQueryHelperService = bigQueryHelperService;
  }

  @Override
  public String getAwsDataSetId(String accountId) {
    return awsBillingMetaDataCache.get(accountId);
  }

  @Override
  public Boolean checkPipelineJobFinished(String accountId, Instant startTime, Instant endTime) {
    CacheKey cacheKey = new CacheKey(accountId, startTime, endTime);
    return pipelineJobStatusCache.get(cacheKey);
  }

  private Boolean getPipelineJobStatus(String accountId, Instant startTime, Instant endTime) {
    String awsDataSetId = getAwsDataSetId(accountId);
    if (awsDataSetId != null) {
      Instant startAt = endTime.minus(1, ChronoUnit.HOURS);
      Map<String, VMInstanceBillingData> awsEC2BillingData =
          bigQueryHelperService.getAwsBillingData(startAt, endTime, awsDataSetId);
      return isNotEmpty(awsEC2BillingData);
    }
    return Boolean.TRUE;
  }

  private String getAwsBillingMetaData(String accountId) {
    if (!ImmutableSet.of("zEaak-FLS425IEO7OLzMUg", "ng2HGKFpStaPsVqGr3B3gA").contains(accountId)) {
      return null;
    }
    List<SettingAttribute> settingAttributes = cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(
        accountId, SettingAttribute.SettingCategory.CE_CONNECTOR, SettingVariableTypes.CE_AWS);
    if (!settingAttributes.isEmpty()) {
      SettingAttribute settingAttribute = settingAttributes.get(0);
      BillingDataPipelineRecord billingDataPipelineRecord =
          billingDataPipelineRecordDao.getBySettingId(accountId, settingAttribute.getUuid());
      return billingDataPipelineRecord.getDataSetId();
    }
    return null;
  }
}
