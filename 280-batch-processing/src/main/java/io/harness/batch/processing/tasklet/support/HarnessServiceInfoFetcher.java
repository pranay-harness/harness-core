package io.harness.batch.processing.tasklet.support;

import static io.harness.batch.processing.writer.constants.K8sCCMConstants.K8SV1_RELEASE_NAME;
import static io.harness.batch.processing.writer.constants.K8sCCMConstants.RELEASE_NAME;

import com.google.inject.Inject;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class HarnessServiceInfoFetcher {
  private final CloudToHarnessMappingService cloudToHarnessMappingService;
  private final K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher;

  private final LoadingCache<CacheKey, HarnessServiceInfo> getHarnessServiceInfoCache;

  @Value
  private static class CacheKey {
    private String accountId;
    private String computeProviderId;
    private String namespace;
    private String podName;
  }

  @Inject
  public HarnessServiceInfoFetcher(K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher,
      CloudToHarnessMappingService cloudToHarnessMappingService) {
    this.k8sLabelServiceInfoFetcher = k8sLabelServiceInfoFetcher;
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
    this.getHarnessServiceInfoCache =
        Caffeine.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .maximumSize(1_000)
            .build(key
                -> this.cloudToHarnessMappingService
                       .getHarnessServiceInfo(key.accountId, key.computeProviderId, key.namespace, key.podName)
                       .orElse(null));
  }

  public Optional<HarnessServiceInfo> fetchHarnessServiceInfo(
      String accountId, String computeProviderId, String namespace, String podName, Map<String, String> labelsMap) {
    try {
      Optional<HarnessServiceInfo> harnessServiceInfo = Optional.empty();
      if (labelsMap.containsKey(K8SV1_RELEASE_NAME) || labelsMap.containsKey(RELEASE_NAME)) {
        harnessServiceInfo = Optional.ofNullable(
            getHarnessServiceInfoCache.get(new CacheKey(accountId, computeProviderId, namespace, podName)));
      }
      if (!harnessServiceInfo.isPresent()) {
        harnessServiceInfo = k8sLabelServiceInfoFetcher.fetchHarnessServiceInfoFromCache(accountId, labelsMap);
      }
      return harnessServiceInfo;
    } catch (Exception ex) {
      logger.error("Error while fetching data {}", ex);
      return Optional.ofNullable(null);
    }
  }
}
