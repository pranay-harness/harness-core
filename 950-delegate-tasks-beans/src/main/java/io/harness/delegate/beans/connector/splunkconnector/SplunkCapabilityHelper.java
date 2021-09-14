/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.splunkconnector;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SplunkCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorConfigDTO, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    SplunkConnectorDTO splunkConnectorDTO = (SplunkConnectorDTO) connectorConfigDTO;
    final String splunkUrl = splunkConnectorDTO.getSplunkUrl();
    capabilityList.add(

        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(splunkUrl, maskingEvaluator));
    populateDelegateSelectorCapability(capabilityList, splunkConnectorDTO.getDelegateSelectors());
    return capabilityList;
  }
}
