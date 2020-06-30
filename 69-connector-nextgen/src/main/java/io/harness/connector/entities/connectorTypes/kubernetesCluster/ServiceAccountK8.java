package io.harness.connector.entities.connectorTypes.kubernetesCluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("serviceAccountK8")
public class ServiceAccountK8 extends KubernetesAuth {
  String serviceAcccountToken;
}