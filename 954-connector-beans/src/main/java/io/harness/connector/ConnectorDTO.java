package io.harness.connector;

import io.harness.EntityType;
import io.harness.yaml.schema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@ApiModel("Connector")
@YamlSchemaRoot(value = EntityType.CONNECTORS, availableAtOrgLevel = true, availableAtAccountLevel = true)
public class ConnectorDTO {
  @JsonProperty("connector") @Valid ConnectorInfoDTO connectorInfo;
}
