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

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectorRequestDTO {
  String name;
  String identifier;
  String description;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifer;
  List<String> tags;

  @JsonProperty("type") ConnectorType connectorType;
  @JsonProperty("type1") ConnectorType connectorType1;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type1", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({ @JsonSubTypes.Type(value = KubernetesClusterConfigDTO.class, name = "K8sCluster") })
  ConnectorConfigDTO connectorConfig;
}