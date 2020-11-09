package io.harness.ngtriggers.beans.target;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TargetType {
  @JsonProperty("Pipeline") PIPELINE
  // add more when more targets are decided on
}
