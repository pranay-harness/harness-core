package io.harness.cvng.beans.appd;

import io.harness.cvng.beans.MetricPackDTO;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AppdynamicsMetricPackDataValidationRequest {
  List<MetricPackDTO> metricPacks;
  AppDynamicsConnectorDTO connector;
}
