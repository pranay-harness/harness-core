package io.harness.cvng.core.beans.params;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@SuperBuilder
public class ProjectParams {
  @NonNull String accountIdentifier;
  @NonNull String orgIdentifier;
  @NonNull String projectIdentifier;
}
