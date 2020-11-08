package io.harness.delegate.beans.ci.status;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildStatusPushResponse implements BuildPushResponse {
  public enum Status {
    SUCCESS,
    ERROR;
  }

  Status status;
}
