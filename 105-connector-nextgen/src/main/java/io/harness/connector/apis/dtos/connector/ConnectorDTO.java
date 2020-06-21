package io.harness.connector.apis.dtos.connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.common.ConnectorType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectorDTO {
  String name;
  String identifier;
  @JsonProperty("kind") ConnectorType connectorType;

  @JsonProperty("spec")
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
  @JsonSubTypes({ @JsonSubTypes.Type(value = KubernetesClusterConfigDTO.class, name = "K8sCluster") })
  ConnectorConfigDTO connectorConfig;
}