package io.harness.beans;

import io.harness.encryption.Scope;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IdentifierRef {
  Scope scope;
  String identifier;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
}
