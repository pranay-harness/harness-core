package io.harness.connector.apis.dto;

import io.harness.EntityType;
import io.harness.yamlSchema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@ApiModel("Connector")
@YamlSchemaRoot(EntityType.CONNECTORS)
public class ConnectorDTO {
  @JsonProperty("connector") ConnectorInfoDTO connectorInfo;
}
