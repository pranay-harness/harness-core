package io.harness.ngtriggers.beans.target;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("targetType")
public enum TargetType {
  @JsonProperty("Pipeline") PIPELINE
  // add more when more targets are decided on
}
