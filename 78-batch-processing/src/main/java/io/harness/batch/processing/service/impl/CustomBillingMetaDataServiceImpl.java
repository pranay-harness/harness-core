package io.harness.batch.processing.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.processor.util.InstanceMetaDataUtils;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CustomBillingMetaDataServiceImpl implements CustomBillingMetaDataService {
  private final CloudToHarnessMappingService cloudToHarnessMappingService;
  private final BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  private final InstanceDataService instanceDataService;
  private final BigQueryHelperService bigQueryHelperService;

  private LoadingCache<String, String> awsBillingMetaDataCache =
      Caffeine.newBuilder().expireAfterWrite(4, TimeUnit.HOURS).build(this ::getAwsBillingMetaData);

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
      BillingDataPipelineRecordDao billingDataPipelineRecordDao, InstanceDataService instanceDataService,
      BigQueryHelperService bigQueryHelperService) {
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
    this.billingDataPipelineRecordDao = billingDataPipelineRecordDao;
    this.instanceDataService = instanceDataService;
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
      InstanceData activeInstance =
          instanceDataService.getActiveInstance(accountId, startTime, endTime, CloudProvider.AWS);
      if (null != activeInstance) {
        Instant startAt = endTime.minus(1, ChronoUnit.HOURS);
        String resourceId = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
            InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, activeInstance);
        Map<String, VMInstanceBillingData> awsEC2BillingData = bigQueryHelperService.getAwsEC2BillingData(
            Collections.singletonList(resourceId), startAt, endTime, awsDataSetId);
        return isNotEmpty(awsEC2BillingData);
      }
    }
    return Boolean.TRUE;
  }

  private String getAwsBillingMetaData(String accountId) {
    if (!accountId.equals("zEaak-FLS425IEO7OLzMUg")) {
      return null;
    }
    List<SettingAttribute> settingAttributes = cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(
        accountId, SettingAttribute.SettingCategory.CE_CONNECTOR, SettingValue.SettingVariableTypes.CE_AWS);
    if (!settingAttributes.isEmpty()) {
      SettingAttribute settingAttribute = settingAttributes.get(0);
      BillingDataPipelineRecord billingDataPipelineRecord =
          billingDataPipelineRecordDao.getBySettingId(accountId, settingAttribute.getUuid());
      return billingDataPipelineRecord.getDataSetId();
    }
    return null;
  }
}
