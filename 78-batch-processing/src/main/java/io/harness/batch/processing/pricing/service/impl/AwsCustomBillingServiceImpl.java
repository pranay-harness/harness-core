package io.harness.batch.processing.pricing.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomBillingService;
import io.harness.batch.processing.processor.util.InstanceMetaDataUtils;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AwsCustomBillingServiceImpl implements AwsCustomBillingService {
  private BigQueryHelperService bigQueryHelperService;

  @Autowired
  public AwsCustomBillingServiceImpl(BigQueryHelperService bigQueryHelperService) {
    this.bigQueryHelperService = bigQueryHelperService;
  }

  private Cache<CacheKey, VMInstanceBillingData> awsResourceBillingCache =
      Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

  @Value
  private static class CacheKey {
    private String resourceId;
    private Instant startTime;
    private Instant endTime;
  }

  public void updateAwsEC2BillingDataCache(
      List<String> resourceIds, Instant startTime, Instant endTime, String dataSetId) {
    Map<String, VMInstanceBillingData> awsEC2BillingData =
        bigQueryHelperService.getAwsEC2BillingData(resourceIds, startTime, endTime, dataSetId);
    awsEC2BillingData.forEach(
        (resourceId, vmInstanceBillingData)
            -> awsResourceBillingCache.put(new CacheKey(resourceId, startTime, endTime), vmInstanceBillingData));
  }

  @Override
  public VMInstanceBillingData getComputeVMPricingInfo(InstanceData instanceData, Instant startTime, Instant endTime) {
    String resourceId = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, instanceData.getMetaData());
    if (null != resourceId) {
      return awsResourceBillingCache.getIfPresent(new CacheKey(resourceId, startTime, endTime));
    }
    return null;
  }

  @Override
  public EcsFargatePricingInfo getFargateVMPricingInfo(InstanceData instanceData, Instant startTime) {
    return null;
  }
}
