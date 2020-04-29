package io.harness.batch.processing.k8s.rcd;

import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetSpec;
import io.kubernetes.client.util.Yaml;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StatefulSetRcdCalculator implements ResourceClaimDiffCalculator {
  @Override
  public String getKind() {
    return "StatefulSet";
  }

  @Override
  public ResourceClaim computeResourceDiff(String oldYaml, String newYaml) {
    Optional<V1StatefulSetSpec> oldStsSpecMaybe =
        Optional.ofNullable(Yaml.loadAs(oldYaml, V1StatefulSet.class)).map(V1StatefulSet::getSpec);
    Optional<V1StatefulSetSpec> newStsSpecMaybe =
        Optional.ofNullable(Yaml.loadAs(newYaml, V1StatefulSet.class)).map(V1StatefulSet::getSpec);
    V1PodSpec oldPodSpec =
        oldStsSpecMaybe.map(V1StatefulSetSpec::getTemplate).map(V1PodTemplateSpec::getSpec).orElse(null);
    V1PodSpec newPodSpec =
        newStsSpecMaybe.map(V1StatefulSetSpec::getTemplate).map(V1PodTemplateSpec::getSpec).orElse(null);
    int oldReplicas = oldStsSpecMaybe.map(V1StatefulSetSpec::getReplicas).orElse(0);
    int newReplicas = newStsSpecMaybe.map(V1StatefulSetSpec::getReplicas).orElse(0);
    return ResourceClaimUtils.forPodWithScale(oldPodSpec, oldReplicas, newPodSpec, newReplicas);
  }
}
