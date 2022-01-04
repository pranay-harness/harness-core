package io.harness.ccm.commons.entities;

import io.harness.connector.ConnectorValidationResult;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CCMConnectorDetails {
  String name;
  Long createdAt;
  ConnectorValidationResult connectorValidationResult;
}
