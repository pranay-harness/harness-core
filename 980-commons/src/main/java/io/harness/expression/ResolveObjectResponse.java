package io.harness.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@OwnedBy(CDC)
@Value
public class ResolveObjectResponse {
  boolean processed;
  boolean changed;
}
