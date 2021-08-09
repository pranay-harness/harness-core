package io.harness.ngtriggers.beans.entity;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class ValidationStatus {
  public enum Status {
    INVALID_TRIGGER_YAML,
    POLLING_SUBSCRIPTION_FAILED,
    POLLING_SUBSCRIPTION_SUCCESS,
  }

  boolean validationFailure;
  Status status;
  String detailMessage;
}
