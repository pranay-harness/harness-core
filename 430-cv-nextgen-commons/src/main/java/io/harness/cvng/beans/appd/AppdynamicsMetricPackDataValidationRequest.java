/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.appd;

import io.harness.cvng.beans.MetricPackDTO;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppdynamicsMetricPackDataValidationRequest {
  List<MetricPackDTO> metricPacks;
  AppDynamicsConnectorDTO connector;
}
