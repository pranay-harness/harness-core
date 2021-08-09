package io.harness.cvng.beans.stackdriver;

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
