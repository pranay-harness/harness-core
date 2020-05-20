package io.harness.batch.processing.pricing.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.pricing.client.BanzaiPricingClient;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.PricingResponse;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class VMPricingServiceImpl implements VMPricingService {
  private final BanzaiPricingClient banzaiPricingClient;

  private static final String COMPUTE_SERVICE = "compute";

  @Autowired
  public VMPricingServiceImpl(BanzaiPricingClient banzaiPricingClient) {
    this.banzaiPricingClient = banzaiPricingClient;
  }

  private Cache<String, VMComputePricingInfo> vmPricingInfoCache = Caffeine.newBuilder().build();

  @Override
  public VMComputePricingInfo getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider) {
    VMComputePricingInfo vmComputePricingInfo = getVMPricingInfoFromCache(instanceType, region, cloudProvider);
    if (null != vmComputePricingInfo) {
      return vmComputePricingInfo;
    } else {
      refreshCache(region, COMPUTE_SERVICE, cloudProvider);
      return getVMPricingInfoFromCache(instanceType, region, cloudProvider);
    }
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
        logger.info("Cache size {}", vmPricingInfoCache.asMap().size());
        logger.info("Pricing response {} {}", pricingInfo.toString(), pricingInfo.body().getProducts());
      } else {
        logger.info("Null response for params {} {} {}", region, serviceName, cloudProvider);
      }
    } catch (IOException e) {
      logger.error("Exception in pricing service ", e);
    }
  }

  String getVMCacheKey(String instanceType, String region, CloudProvider cloudProvider) {
    return "id_" + md5Hex(("i_" + instanceType + "r_" + region + "c_" + cloudProvider).getBytes(UTF_8));
  }
}
