package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StackdriverLogDefinition {
  String name;
  String query;
  String messageIdentifier;
  String serviceInstanceIdentifier;
}
