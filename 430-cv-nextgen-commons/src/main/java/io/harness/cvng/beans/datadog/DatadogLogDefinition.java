package io.harness.cvng.beans.datadog;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DatadogLogDefinition {
  String name;
  String query;
  List<String> indexes;
  String serviceInstanceIdentifier;
}
