package io.harness.delegate.task.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@OwnedBy(HarnessTeam.CDP)
public enum TFTaskType {
  @JsonProperty("Apply") APPLY("Apply"),
  @JsonProperty("Destroy") DESTROY("Destroy"),
  @JsonProperty("Plan") PLAN("Plan");

  @Getter private final String displayName;
  TFTaskType(String displayName) {
    this.displayName = displayName;
  }
}
