package io.harness.batch.processing.processor.support;

import com.google.inject.Inject;

import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import org.springframework.stereotype.Component;
import software.wings.api.DeploymentSummary;
import software.wings.api.K8sDeploymentInfo;
import software.wings.beans.infrastructure.instance.key.deployment.K8sDeploymentKey;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.Map;
import java.util.Optional;

/**
 * Maps k8s objects to harness services using labels.
 */
@Component
public class K8sLabelServiceInfoFetcher {
  private final CloudToHarnessMappingService cloudToHarnessMappingService;

  @Inject
  public K8sLabelServiceInfoFetcher(CloudToHarnessMappingService cloudToHarnessMappingService) {
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
  }

  public Optional<HarnessServiceInfo> fetchHarnessServiceInfo(String accountId, Map<String, String> labelsMap) {
    return Optional.ofNullable(labelsMap.get(K8sCCMConstants.RELEASE_NAME)).flatMap(relName -> {
      K8sDeploymentKey k8sDeploymentKey = K8sDeploymentKey.builder().releaseName(relName).build();
      K8sDeploymentInfo k8sDeploymentInfo = K8sDeploymentInfo.builder().releaseName(relName).build();
      DeploymentSummary deploymentSummary = DeploymentSummary.builder()
                                                .accountId(accountId)
                                                .k8sDeploymentKey(k8sDeploymentKey)
                                                .deploymentInfo(k8sDeploymentInfo)
                                                .build();
      return cloudToHarnessMappingService.getHarnessServiceInfo(deploymentSummary);
    });
  }
}
