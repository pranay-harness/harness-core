package io.harness.cvng.beans.datadog;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@Value
@Builder
@EqualsAndHashCode
public class DatadogLogDefinition {
  String name;
  String query;
  List<String> indexes;
  String serviceInstanceIdentifier;
}
