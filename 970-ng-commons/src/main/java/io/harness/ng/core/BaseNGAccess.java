package io.harness.ng.core;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
public class BaseNGAccess implements NGAccess {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
}
