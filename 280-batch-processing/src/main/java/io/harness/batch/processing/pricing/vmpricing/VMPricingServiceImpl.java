package io.harness.batch.processing.pricing.vmpricing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

import io.harness.batch.processing.pricing.service.support.GCPCustomInstanceDetailProvider;
import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.pricing.client.CloudInfoPricingClient;
import io.harness.pricing.dto.cloudinfo.ProductDetails;
import io.harness.pricing.dto.cloudinfo.ProductDetailsResponse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

@Service
@Slf4j
public class VMPricingServiceImpl implements VMPricingService {
  private final CloudInfoPricingClient banzaiPricingClient;

  private static final String COMPUTE_SERVICE = "compute";

  @Autowired
  public VMPricingServiceImpl(CloudInfoPricingClient banzaiPricingClient) {
    this.banzaiPricingClient = banzaiPricingClient;
  }

  private final Cache<String, ProductDetails> vmPricingInfoCache = Caffeine.newBuilder().build();

  @Override
  public ProductDetails getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider) {
    if (ImmutableSet.of("switzerlandnorth", "switzerlandwest", "germanywestcentral").contains(region)) {
      region = "uksouth";
    }

    ProductDetails vmComputePricingInfo = getVMPricingInfoFromCacheIfPresent(instanceType, region, cloudProvider);

    if (vmComputePricingInfo == null
        && (ImmutableSet.of("n2-standard-16", "n2-standard-2").contains(instanceType)
            || GCPCustomInstanceDetailProvider.isCustomGCPInstance(instanceType, cloudProvider))) {
      vmComputePricingInfo = GCPCustomInstanceDetailProvider.getCustomVMPricingInfo(instanceType, region);
    }

    if (null == vmComputePricingInfo) {
      refreshCache(region, COMPUTE_SERVICE, cloudProvider);
      vmComputePricingInfo = getVMPricingInfoFromCacheIfPresent(instanceType, region, cloudProvider);
    }

    return vmComputePricingInfo;
  }

  @Override
  public io.harness.batch.processing.pricing.vmpricing.EcsFargatePricingInfo getFargatePricingInfo(String region) {
    return EcsFargatePricingInfo.builder().region(region).cpuPrice(0.04656).memoryPrice(0.00511).build();
  }

  private ProductDetails getVMPricingInfoFromCacheIfPresent(
      String instanceType, String region, CloudProvider cloudProvider) {
    String vmCacheKey = getVMCacheKey(instanceType, region, cloudProvider);
    return vmPricingInfoCache.getIfPresent(vmCacheKey);
  }

  private void refreshCache(String region, String serviceName, CloudProvider cloudProvider) {
    try {
      Call<ProductDetailsResponse> pricingInfoCall =
          banzaiPricingClient.getPricingInfo(cloudProvider.getCloudProviderName(), serviceName, region);
      Response<ProductDetailsResponse> pricingInfo = pricingInfoCall.execute();
      if (null != pricingInfo.body() && null != pricingInfo.body().getProducts()) {
        List<ProductDetails> products = pricingInfo.body().getProducts();
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
