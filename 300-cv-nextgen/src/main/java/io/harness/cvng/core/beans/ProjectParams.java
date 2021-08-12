package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ProjectParams {
  @NonNull String accountIdentifier;
  @NonNull String orgIdentifier;
  @NonNull String projectIdentifier;
}
