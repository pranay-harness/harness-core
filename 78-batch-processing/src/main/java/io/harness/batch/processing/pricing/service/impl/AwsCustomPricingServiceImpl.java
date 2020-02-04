package io.harness.batch.processing.pricing.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.aws.athena.AwsAthenaQueryHelperService;
import io.harness.batch.processing.pricing.data.AccountComputePricingData;
import io.harness.batch.processing.pricing.data.AccountFargatePricingData;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo.EcsFargatePricingInfoBuilder;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomPricingService;
import io.harness.batch.processing.service.impl.SettingValueServiceImpl;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.ccm.BillingReportConfig;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.beans.AwsConfig;
import software.wings.settings.SettingValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AwsCustomPricingServiceImpl implements AwsCustomPricingService {
  @Autowired private AwsAthenaQueryHelperService awsAthenaQueryHelperService;
  @Autowired private SettingValueServiceImpl settingValueService;
  @Autowired private HPersistence persistence;

  private Cache<String, VMComputePricingInfo> awsVmPricingInfoCache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  private Cache<String, EcsFargatePricingInfo> ecsFargatePricingInfoCache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  @Override
  public VMComputePricingInfo getComputeVMPricingInfo(InstanceData instanceData, Instant startTime) {
    Map<String, String> instanceMetaData = instanceData.getMetaData();
    String instanceFamily = instanceMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY);
    String operatingSystem = instanceMetaData.get(InstanceMetaDataConstants.OPERATING_SYSTEM);
    String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
    String settingId = instanceData.getSettingId();
    SettingValue settingValue = settingValueService.getSettingValueService(settingId);
    BillingReportConfig billingReportConfig = ((AwsConfig) settingValue).getCcmConfig().getBillingReportConfig();
    if (billingReportConfig.isBillingReportEnabled()) {
      String billingAccountId = billingReportConfig.getBillingAccountId();
      VMComputePricingInfo vmComputePricingInfo =
          getVMPricingInfoFromCache(billingAccountId, instanceFamily, region, operatingSystem);
      if (null != vmComputePricingInfo) {
        return vmComputePricingInfo;
      } else {
        refreshCache(instanceData, billingAccountId, startTime);
        return getVMPricingInfoFromCache(billingAccountId, instanceFamily, region, operatingSystem);
      }
    }
    return null;
  }

  @Override
  public EcsFargatePricingInfo getFargateVMPricingInfo(InstanceData instanceData, Instant startTime) {
    Map<String, String> instanceMetaData = instanceData.getMetaData();
    String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
    String settingId = instanceData.getSettingId();
    SettingValue settingValue = settingValueService.getSettingValueService(settingId);
    BillingReportConfig billingReportConfig = ((AwsConfig) settingValue).getCcmConfig().getBillingReportConfig();
    String billingAccountId = billingReportConfig.getBillingAccountId();
    if (billingReportConfig.isBillingReportEnabled()) {
      EcsFargatePricingInfo fargatePricingInfoFromCache = getFargatePricingInfoFromCache(billingAccountId, region);
      if (null != fargatePricingInfoFromCache) {
        return fargatePricingInfoFromCache;
      } else {
        refreshFargateCache(instanceData, billingAccountId, startTime);
        return getFargatePricingInfoFromCache(billingAccountId, region);
      }
    }
    return null;
  }

  private VMComputePricingInfo getVMPricingInfoFromCache(
      String billingAccountId, String instanceType, String region, String operatingSystem) {
    String vmCacheKey = getVMCacheKey(billingAccountId, instanceType, region, operatingSystem);
    return awsVmPricingInfoCache.getIfPresent(vmCacheKey);
  }

  private EcsFargatePricingInfo getFargatePricingInfoFromCache(String billingAccountId, String region) {
    String vmCacheKey = getFargateCacheKey(billingAccountId, region);
    return ecsFargatePricingInfoCache.getIfPresent(vmCacheKey);
  }

  private void refreshCache(InstanceData instanceData, String billingAccountId, Instant startTime) {
    try {
      List<AccountComputePricingData> accountComputePricingDataList =
          awsAthenaQueryHelperService.fetchComputePriceRate(instanceData.getSettingId(), billingAccountId, startTime);
      accountComputePricingDataList.forEach(accountComputePricingData
          -> awsVmPricingInfoCache.put(
              getVMCacheKey(billingAccountId, accountComputePricingData.getInstanceType(),
                  accountComputePricingData.getRegion(), accountComputePricingData.getOperatingSystem()),
              getVMComputePricingInfo(accountComputePricingData)));
    } catch (Exception e) {
      logger.error("Exception in pricing service ", e);
    }
  }

  private void refreshFargateCache(InstanceData instanceData, String billingAccountId, Instant startTime) {
    try {
      List<AccountFargatePricingData> accountFargatePricingDataList =
          awsAthenaQueryHelperService.fetchEcsFargatePriceRate(
              instanceData.getSettingId(), billingAccountId, startTime);
      List<EcsFargatePricingInfo> ecsFargatePricingInfoList = fetchEcsFargatePricingInfo(accountFargatePricingDataList);
      ecsFargatePricingInfoList.forEach(accountFargatePricingData
          -> ecsFargatePricingInfoCache.put(
              getFargateCacheKey(billingAccountId, accountFargatePricingData.getRegion()), accountFargatePricingData));
    } catch (Exception e) {
      logger.error("Exception in pricing service ", e);
    }
  }

  private List<EcsFargatePricingInfo> fetchEcsFargatePricingInfo(
      List<AccountFargatePricingData> accountFargatePricingDataList) {
    Map<String, List<AccountFargatePricingData>> regionFargatePriceData =
        accountFargatePricingDataList.stream().collect(Collectors.groupingBy(AccountFargatePricingData::getRegion));
    List<EcsFargatePricingInfo> ecsFargatePricingInfoList = new ArrayList<>();
    regionFargatePriceData.forEach((region, regionAccountFargatePricingDataList) -> {
      ecsFargatePricingInfoList.add(getEcsFargatePricingInfo(region, regionAccountFargatePricingDataList));
    });
    return ecsFargatePricingInfoList;
  }

  private EcsFargatePricingInfo getEcsFargatePricingInfo(
      String region, List<AccountFargatePricingData> accountFargatePricingDataList) {
    EcsFargatePricingInfoBuilder ecsFargatePricingInfoBuilder = EcsFargatePricingInfo.builder().region(region);
    accountFargatePricingDataList.forEach(accountFargatePricingData -> {
      if (accountFargatePricingData.isCpuPriceType()) {
        ecsFargatePricingInfoBuilder.cpuPrice(accountFargatePricingData.getUnBlendedRate());
      } else if (accountFargatePricingData.isMemoryPriceType()) {
        ecsFargatePricingInfoBuilder.memoryPrice(accountFargatePricingData.getUnBlendedRate());
      }
    });
    return ecsFargatePricingInfoBuilder.build();
  }

  private VMComputePricingInfo getVMComputePricingInfo(AccountComputePricingData accountComputePricingData) {
    return VMComputePricingInfo.builder()
        .type(accountComputePricingData.getInstanceType())
        .onDemandPrice(accountComputePricingData.getBlendedRate())
        .memPerVm(accountComputePricingData.getMemPerVm())
        .cpusPerVm(accountComputePricingData.getCpusPerVm())
        .build();
  }

  String getVMCacheKey(String billingAccountId, String instanceType, String region, String operatingSystem) {
    return "id_"
        + md5Hex(
              ("b_" + billingAccountId + "i_" + instanceType + "r_" + region + "o_" + operatingSystem).getBytes(UTF_8));
  }

  String getFargateCacheKey(String billingAccountId, String region) {
    return "id_" + md5Hex(("b_" + billingAccountId + "r_" + region).getBytes(UTF_8));
  }
}
