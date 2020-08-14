package io.harness.connector.apis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.connector.entities.ConnectorConnectivityDetails;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectorDTO {
  String name;
  String identifier;
  String description;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  List<String> tags;

  @NotNull @JsonProperty("type") ConnectorType connectorType;

  @Builder
  public ConnectorDTO(String name, String identifier, String description, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, List<String> tags, ConnectorType connectorType,
      ConnectorConfigDTO connectorConfig, Long createdAt, Long lastModifiedAt, ConnectorConnectivityDetails status) {
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.tags = tags;
    this.connectorType = connectorType;
    this.connectorConfig = connectorConfig;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
    this.status = status;
  }

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  ConnectorConfigDTO connectorConfig;
  Long createdAt;
  Long lastModifiedAt;
  ConnectorConnectivityDetails status;
}
