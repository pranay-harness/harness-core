package io.harness.cvng.beans.datadog;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class DatadogLogDefinition {
  String name;
  String query;
  List<String> indexes;
  String serviceInstanceIdentifier;
}
