package io.harness.batch.processing.processor.support;

import static io.harness.batch.processing.writer.constants.K8sCCMConstants.HELM_RELEASE_NAME;
import static io.harness.batch.processing.writer.constants.K8sCCMConstants.RELEASE_NAME;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.wings.api.DeploymentSummary;
import software.wings.api.DeploymentSummary.DeploymentSummaryBuilder;
import software.wings.api.K8sDeploymentInfo;
import software.wings.beans.container.Label;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.K8sDeploymentKey;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Maps k8s objects to harness services using labels.
 */
@Slf4j
@Component
public class K8sLabelServiceInfoFetcher {
  private final CloudToHarnessMappingService cloudToHarnessMappingService;

  @Inject
  public K8sLabelServiceInfoFetcher(CloudToHarnessMappingService cloudToHarnessMappingService) {
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
  }

  public Optional<HarnessServiceInfo> fetchHarnessServiceInfo(String accountId, Map<String, String> labelsMap) {
    DeploymentSummaryBuilder deploymentSummaryBuilder = DeploymentSummary.builder().accountId(accountId);
    if (labelsMap.containsKey(RELEASE_NAME)) {
      String relName = labelsMap.get(RELEASE_NAME);
      K8sDeploymentKey k8sDeploymentKey = K8sDeploymentKey.builder().releaseName(relName).build();
      K8sDeploymentInfo k8sDeploymentInfo = K8sDeploymentInfo.builder().releaseName(relName).build();
      deploymentSummaryBuilder.k8sDeploymentKey(k8sDeploymentKey).deploymentInfo(k8sDeploymentInfo);
      return cloudToHarnessMappingService.getHarnessServiceInfo(deploymentSummaryBuilder.build());
    } else if (labelsMap.containsKey(HELM_RELEASE_NAME)) {
      String relName = labelsMap.get(HELM_RELEASE_NAME);
      Label label = Label.Builder.aLabel().withName(HELM_RELEASE_NAME).withValue(relName).build();
      ContainerDeploymentKey containerDeploymentKey =
          ContainerDeploymentKey.builder().labels(Arrays.asList(label)).build();
      deploymentSummaryBuilder.containerDeploymentKey(containerDeploymentKey);
      DeploymentSummary deploymentSummary = deploymentSummaryBuilder.build();
      return cloudToHarnessMappingService.getHarnessServiceInfo(deploymentSummary);
    } else {
      return Optional.empty();
    }
  }
}
