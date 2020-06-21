package io.harness.connector.apis.dtos.K8Connector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.connector.apis.dtos.connector.ConnectorConfigDTO;
import io.harness.connector.common.kubernetes.KubernetesCredentialType;
import lombok.Builder;
import lombok.Value;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterConfigDTO implements ConnectorConfigDTO {
  @JsonProperty("kind") KubernetesCredentialType kubernetesCredentialType;

  @JsonProperty("spec")
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
  @JsonSubTypes({ @JsonSubTypes.Type(value = KubernetesDelegateDetailsDTO.class, name = "InheritFromDelegate") })
  KubernetesCredentialDTO config;
}