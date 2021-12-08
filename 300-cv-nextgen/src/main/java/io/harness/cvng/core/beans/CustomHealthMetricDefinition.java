package io.harness.cvng.core.beans;

import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomHealthMetricDefinition extends HealthSourceMetricDefinition {
  String groupName;
  HealthSourceQueryType queryType;
  String urlPath;
  CustomHealthMethod method;
  String requestBody;

  String timestampFieldPathString;
  String timestampFormat;
  String metricValueFieldPathString;
  String serviceInstance;
}
