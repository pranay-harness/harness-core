package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "KubernetesActivitySourceDTOKeys")
public class KubernetesActivitySourceDTO {
  String namespace;
  String clusterName;
  String workloadName;
}
