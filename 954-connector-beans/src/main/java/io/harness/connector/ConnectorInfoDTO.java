package io.harness.connector;

import static io.harness.ConnectorConstants.CONNECTOR_TYPES;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectorInfoDTO {
  @NotNull @NotBlank @ApiModelProperty(value = "Name of the connector", position = 2) String name;
  @NotNull
  @NotBlank
  @EntityIdentifier
  @ApiModelProperty(value = "Identifier of the connector", position = 1)
  String identifier;
  String description;
  String orgIdentifier;
  String projectIdentifier;
  Map<String, String> tags;

  @NotNull
  @JsonProperty(CONNECTOR_TYPES)
  @ApiModelProperty("Type of connector")
  io.harness.delegate.beans.connector.ConnectorType connectorType;

  @JsonProperty("spec")
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = CONNECTOR_TYPES, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true)
  @Valid
  @NotNull
  io.harness.delegate.beans.connector.ConnectorConfigDTO connectorConfig;

  // Adding custom setters for Jackson to set empty string as null
  public void setOrgIdentifier(String orgIdentifier) {
    this.orgIdentifier = isEmpty(orgIdentifier) ? null : orgIdentifier;
  }

  public void setProjectIdentifier(String projectIdentifier) {
    this.projectIdentifier = isEmpty(projectIdentifier) ? null : projectIdentifier;
  }

  @Builder
  public ConnectorInfoDTO(String name, String identifier, String description, String orgIdentifier,
      String projectIdentifier, Map<String, String> tags, ConnectorType connectorType,
      ConnectorConfigDTO connectorConfig) {
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.orgIdentifier = isEmpty(orgIdentifier) ? null : orgIdentifier;
    this.projectIdentifier = isEmpty(projectIdentifier) ? null : projectIdentifier;
    this.tags = tags;
    this.connectorType = connectorType;
    this.connectorConfig = connectorConfig;
  }
}
