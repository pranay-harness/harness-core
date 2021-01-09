package io.harness.connector;

import static io.harness.ConnectorConstants.CONNECTOR_TYPES;

import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ExecutionCapabilityDemanderWithScope;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectorInfoDTO {
  @NotNull String name;
  @NotNull @EntityIdentifier String identifier;
  String description;
  String orgIdentifier;
  String projectIdentifier;
  Map<String, String> tags;

  @NotNull @JsonProperty(CONNECTOR_TYPES) ConnectorType connectorType;

  @JsonProperty("spec")
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = CONNECTOR_TYPES, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true)
  @Valid
  @NotNull
  ConnectorConfigDTO connectorConfig;

  @Builder
  public ConnectorInfoDTO(String name, String identifier, String description, String orgIdentifier,
      String projectIdentifier, Map<String, String> tags, ConnectorType connectorType,
      ConnectorConfigDTO connectorConfig) {
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.tags = tags;
    this.connectorType = connectorType;
    this.connectorConfig = connectorConfig;
  }

  public List<ExecutionCapability> fetchRequiredExecutionCapabilitiesForConnector(
      String accountIdentifier, ExpressionEvaluator maskingEvaluator) {
    if (this.connectorConfig instanceof ExecutionCapabilityDemanderWithScope) {
      return ((ExecutionCapabilityDemanderWithScope) this.connectorConfig)
          .fetchRequiredExecutionCapabilities(accountIdentifier, orgIdentifier, projectIdentifier);
    } else if (this.connectorConfig instanceof ExecutionCapabilityDemander) {
      return ((ExecutionCapabilityDemander) this.connectorConfig).fetchRequiredExecutionCapabilities(maskingEvaluator);
    }
    throw new UnsupportedOperationException(
        "The execution capability is not defined for the connector type " + connectorType);
  }
}
