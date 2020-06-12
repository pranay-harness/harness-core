package io.harness.beans.yaml.extended;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomVariables {
  String name;
  String type;
  String value;
}
