package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterConfigDTO extends ConnectorConfigDTO {
  KubernetesCredentialDTO credential;
}