/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.k8s.rcd;

import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicaSetSpec;
import io.kubernetes.client.util.Yaml;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReplicaSetRcdCalculator implements ResourceClaimDiffCalculator {
  @Override
  public String getKind() {
    return "ReplicaSet";
  }

  @Override
  public ResourceClaimDiff computeResourceClaimDiff(String oldYaml, String newYaml) {
    Optional<V1ReplicaSetSpec> oldRsSpecMaybe =
        Optional.ofNullable(Yaml.loadAs(oldYaml, V1ReplicaSet.class)).map(V1ReplicaSet::getSpec);
    Optional<V1ReplicaSetSpec> newRsSpecMaybe =
        Optional.ofNullable(Yaml.loadAs(newYaml, V1ReplicaSet.class)).map(V1ReplicaSet::getSpec);
    V1PodSpec oldPodSpec =
        oldRsSpecMaybe.map(V1ReplicaSetSpec::getTemplate).map(V1PodTemplateSpec::getSpec).orElse(null);
    V1PodSpec newPodSpec =
        newRsSpecMaybe.map(V1ReplicaSetSpec::getTemplate).map(V1PodTemplateSpec::getSpec).orElse(null);
    int oldReplicas = oldRsSpecMaybe.map(V1ReplicaSetSpec::getReplicas).orElse(0);
    int newReplicas = newRsSpecMaybe.map(V1ReplicaSetSpec::getReplicas).orElse(0);
    return ResourceClaimUtils.resourceClaimDiffForPodWithScale(oldPodSpec, oldReplicas, newPodSpec, newReplicas);
  }
}
