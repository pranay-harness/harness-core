package io.harness.connector.apis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectorDTO {
  String name;
  String identifier;
  String description;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifer;
  List<String> tags;

  @JsonProperty("type1") ConnectorType connectorType1;
  @JsonProperty("type") ConnectorType connectorType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type1", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({ @JsonSubTypes.Type(value = KubernetesClusterConfigDTO.class, name = "K8sCluster") })
  ConnectorConfigDTO connectorConfig;
  Long createdAt;
  Long lastModifiedAt;
}