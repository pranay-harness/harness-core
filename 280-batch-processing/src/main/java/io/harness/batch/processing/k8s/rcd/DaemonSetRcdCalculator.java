package io.harness.batch.processing.k8s.rcd;

import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1DaemonSetSpec;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.util.Yaml;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DaemonSetRcdCalculator implements ResourceClaimDiffCalculator {
  @Override
  public String getKind() {
    return "DaemonSet";
  }

  @Override
  public ResourceClaimDiff computeResourceClaimDiff(String oldYaml, String newYaml) {
    // We're not taking into account the number of pods of the daemonset that are running (which depends on things like
    // number of nodes in the cluster satisfying the node selector) since it's not available from the spec alone
    V1PodSpec oldPodSpec = Optional.ofNullable(Yaml.loadAs(oldYaml, V1DaemonSet.class))
                               .map(V1DaemonSet::getSpec)
                               .map(V1DaemonSetSpec::getTemplate)
                               .map(V1PodTemplateSpec::getSpec)
                               .orElse(null);
    V1PodSpec newPodSpec = Optional.ofNullable(Yaml.loadAs(newYaml, V1DaemonSet.class))
                               .map(V1DaemonSet::getSpec)
                               .map(V1DaemonSetSpec::getTemplate)
                               .map(V1PodTemplateSpec::getSpec)
                               .orElse(null);
    return ResourceClaimUtils.resourceClaimDiffForPod(oldPodSpec, newPodSpec);
  }
}
