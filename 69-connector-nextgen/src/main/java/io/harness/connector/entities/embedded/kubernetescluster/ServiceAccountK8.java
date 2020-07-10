package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("serviceAccountK8")
public class ServiceAccountK8 implements KubernetesAuth {
  String serviceAcccountToken;
}