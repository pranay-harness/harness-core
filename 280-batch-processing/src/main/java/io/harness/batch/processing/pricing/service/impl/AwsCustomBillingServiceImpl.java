package io.harness.batch.processing.pricing.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomBillingService;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils;
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
  private InstanceDataService instanceDataService;

  @Autowired
  public AwsCustomBillingServiceImpl(
      BigQueryHelperService bigQueryHelperService, InstanceDataService instanceDataService) {
    this.bigQueryHelperService = bigQueryHelperService;
    this.instanceDataService = instanceDataService;
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
    String resourceId = getResourceId(instanceData);
    if (null != resourceId) {
      return awsResourceBillingCache.getIfPresent(new CacheKey(resourceId, startTime, endTime));
    }
    return null;
  }

  String getResourceId(InstanceData instanceData) {
    String resourceId = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, instanceData.getMetaData());
    if (null == resourceId && instanceData.getInstanceType() == InstanceType.K8S_POD) {
      String parentResourceId = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
          InstanceMetaDataConstants.ACTUAL_PARENT_RESOURCE_ID, instanceData);
      InstanceData parentInstanceData = null;
      if (null != parentResourceId) {
        parentInstanceData = instanceDataService.fetchInstanceData(parentResourceId);
      } else {
        parentResourceId = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
            InstanceMetaDataConstants.PARENT_RESOURCE_ID, instanceData);
        if (null != parentResourceId) {
          parentInstanceData = instanceDataService.fetchInstanceDataWithName(
              instanceData.getAccountId(), instanceData.getClusterId(), parentResourceId, Instant.now().toEpochMilli());
        }
      }
      if (null != parentInstanceData) {
        resourceId = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
            InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, parentInstanceData.getMetaData());
      }
    }
    return resourceId;
  }

  @Override
  public EcsFargatePricingInfo getFargateVMPricingInfo(InstanceData instanceData, Instant startTime) {
    return null;
  }
}
