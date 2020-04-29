package io.harness.batch.processing.k8s.rcd;

import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.util.Yaml;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PodRcdCalculator implements ResourceClaimDiffCalculator {
  @Override
  public String getKind() {
    return "Pod";
  }

  @Override
  public ResourceClaim computeResourceDiff(String oldYaml, String newYaml) {
    V1PodSpec oldPodSpec = Optional.ofNullable(Yaml.loadAs(oldYaml, V1Pod.class)).map(V1Pod::getSpec).orElse(null);
    V1PodSpec newPodSpec = Optional.ofNullable(Yaml.loadAs(newYaml, V1Pod.class)).map(V1Pod::getSpec).orElse(null);
    return ResourceClaimUtils.forPod(oldPodSpec, newPodSpec);
  }
}
