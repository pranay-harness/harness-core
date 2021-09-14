/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorCapabilitiesHelper;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataCollectionConnectorBundle implements ExecutionCapabilityDemander {
  // JSON serialization does not work for ConnectorConfigDTO without the wrapper so need to pass the whole object
  private String connectorIdentifier;
  private String sourceIdentifier;
  private String dataCollectionWorkerId;
  private ConnectorInfoDTO connectorDTO;
  private DataCollectionType dataCollectionType;

  @JsonIgnore
  public ConnectorConfigDTO getConnectorConfigDTO() {
    return connectorDTO.getConnectorConfig();
  }
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return CVConnectorCapabilitiesHelper.fetchRequiredExecutionCapabilities(
        connectorDTO.getConnectorConfig(), maskingEvaluator);
  }
}
