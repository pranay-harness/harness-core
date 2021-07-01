package io.harness.batch.processing.pricing.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

import io.harness.batch.processing.pricing.client.BanzaiPricingClient;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.PricingResponse;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.data.ZonePrice;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.ccm.commons.constants.CloudProvider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

@Service
@Slf4j
public class VMPricingServiceImpl implements VMPricingService {
  private final BanzaiPricingClient banzaiPricingClient;

  private static final String COMPUTE_SERVICE = "compute";
  private static final String COMPUTE_CATEGORY = "General purpose";

  @Autowired
  public VMPricingServiceImpl(BanzaiPricingClient banzaiPricingClient) {
    this.banzaiPricingClient = banzaiPricingClient;
  }

  private Cache<String, VMComputePricingInfo> vmPricingInfoCache = Caffeine.newBuilder().build();

  @Override
  public VMComputePricingInfo getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider) {
    if (ImmutableSet.of("switzerlandnorth", "switzerlandwest", "germanywestcentral").contains(region)) {
      region = "uksouth";
    }
    VMComputePricingInfo vmComputePricingInfo = getVMPricingInfoFromCache(instanceType, region, cloudProvider);
    if (null != vmComputePricingInfo) {
      return vmComputePricingInfo;
    } else if (ImmutableSet.of("n2-standard-16").contains(instanceType)) {
      return getCustomComputeVMPricingInfo(instanceType, region, cloudProvider);
    } else if (ImmutableSet.of("n2-standard-2").contains(instanceType)) {
      return getCustomComputeN2VMPricingInfo(instanceType, region, cloudProvider);
    } else {
      refreshCache(region, COMPUTE_SERVICE, cloudProvider);
      return getVMPricingInfoFromCache(instanceType, region, cloudProvider);
    }
  }

  private VMComputePricingInfo getCustomComputeVMPricingInfo(
      String instanceType, String region, CloudProvider cloudProvider) {
    VMComputePricingInfo vmComputePricingInfo =
        VMComputePricingInfo.builder()
            .category(COMPUTE_CATEGORY)
            .type(instanceType)
            .onDemandPrice(0.7769)
            .spotPrice(getZonePriceList(0.1880, region, ImmutableList.of("a", "b", "c")))
            .networkPrice(0.0)
            .cpusPerVm(16)
            .memPerVm(64)
            .build();
    vmPricingInfoCache.put(getVMCacheKey(instanceType, region, cloudProvider), vmComputePricingInfo);
    return vmComputePricingInfo;
  }

  private VMComputePricingInfo getCustomComputeN2VMPricingInfo(
      String instanceType, String region, CloudProvider cloudProvider) {
    VMComputePricingInfo vmComputePricingInfo =
        VMComputePricingInfo.builder()
            .category(COMPUTE_CATEGORY)
            .type(instanceType)
            .onDemandPrice(0.097118)
            .spotPrice(getZonePriceList(0.02354, region, ImmutableList.of("a", "b", "c")))
            .networkPrice(0.0)
            .cpusPerVm(2)
            .memPerVm(8)
            .build();
    vmPricingInfoCache.put(getVMCacheKey(instanceType, region, cloudProvider), vmComputePricingInfo);
    return vmComputePricingInfo;
  }

  private List<ZonePrice> getZonePriceList(double spotPrice, String region, List<String> zones) {
    return zones.stream()
        .map(zone -> ZonePrice.builder().price(spotPrice).zone(region + "-" + zone).build())
        .collect(Collectors.toList());
  }

  @Override
  public EcsFargatePricingInfo getFargatePricingInfo(String region) {
    return EcsFargatePricingInfo.builder().region(region).cpuPrice(0.04656).memoryPrice(0.00511).build();
  }

  private VMComputePricingInfo getVMPricingInfoFromCache(
      String instanceType, String region, CloudProvider cloudProvider) {
    String vmCacheKey = getVMCacheKey(instanceType, region, cloudProvider);
    return vmPricingInfoCache.getIfPresent(vmCacheKey);
  }

  private void refreshCache(String region, String serviceName, CloudProvider cloudProvider) {
    try {
      Call<PricingResponse> pricingInfoCall =
          banzaiPricingClient.getPricingInfo(cloudProvider.getCloudProviderName(), serviceName, region);
      Response<PricingResponse> pricingInfo = pricingInfoCall.execute();
      if (null != pricingInfo.body() && null != pricingInfo.body().getProducts()) {
        List<VMComputePricingInfo> products = pricingInfo.body().getProducts();
        products.forEach(
            product -> vmPricingInfoCache.put(getVMCacheKey(product.getType(), region, cloudProvider), product));
        log.info("Cache size {}", vmPricingInfoCache.asMap().size());
        log.debug("Pricing response {} {}", pricingInfo.toString(), pricingInfo.body().getProducts());
      } else {
        log.info("Null response for params {} {} {}", region, serviceName, cloudProvider);
      }
    } catch (IOException e) {
      log.error("Exception in pricing service ", e);
    }
  }

  String getVMCacheKey(String instanceType, String region, CloudProvider cloudProvider) {
    return "id_" + md5Hex(("i_" + instanceType + "r_" + region + "c_" + cloudProvider).getBytes(UTF_8));
  }
}
