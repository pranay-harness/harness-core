package io.harness.ccm;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class K8sEventCollectionBundle {
  // this identifier should belong to io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO
  @NotNull String connectorIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  String clusterId;
  String cloudProviderId;
  String clusterName;
}
