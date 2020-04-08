package io.harness.batch.processing.processor.support;

import static io.harness.batch.processing.writer.constants.K8sCCMConstants.K8SV1_RELEASE_NAME;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HarnessServiceInfoFetcher {
  private final CloudToHarnessMappingService cloudToHarnessMappingService;
  private final K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher;

  @Inject
  public HarnessServiceInfoFetcher(K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher,
      CloudToHarnessMappingService cloudToHarnessMappingService) {
    this.k8sLabelServiceInfoFetcher = k8sLabelServiceInfoFetcher;
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
  }

  public Optional<HarnessServiceInfo> fetchHarnessServiceInfo(
      String accountId, String computeProviderId, String namespace, String podName, Map<String, String> labelsMap) {
    try {
      Optional<HarnessServiceInfo> harnessServiceInfo =
          k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(accountId, labelsMap);
      if (!harnessServiceInfo.isPresent() && labelsMap.containsKey(K8SV1_RELEASE_NAME)) {
        harnessServiceInfo =
            cloudToHarnessMappingService.getHarnessServiceInfo(accountId, computeProviderId, namespace, podName);
      }
      return harnessServiceInfo;
    } catch (Exception ex) {
      logger.error("Error while fetching data {}", ex);
      return Optional.ofNullable(null);
    }
  }
}
