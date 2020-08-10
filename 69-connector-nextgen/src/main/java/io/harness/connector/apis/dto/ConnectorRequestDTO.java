package io.harness.connector.apis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

import java.util.List;
import javax.validation.Valid;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectorRequestDTO {
  @NotBlank String name;
  @NotBlank String identifier;
  String description;
  String orgIdentifier;
  String projectIdentifier;
  List<String> tags;

  @JsonProperty("type") ConnectorType connectorType;

  @Builder
  public ConnectorRequestDTO(String name, String identifier, String description, String orgIdentifier,
      String projectIdentifier, List<String> tags, ConnectorType connectorType, ConnectorConfigDTO connectorConfig) {
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.tags = tags;
    this.connectorType = connectorType;
    this.connectorConfig = connectorConfig;
  }

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  ConnectorConfigDTO connectorConfig;
}