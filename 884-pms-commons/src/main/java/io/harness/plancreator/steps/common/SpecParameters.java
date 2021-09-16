package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;

@OwnedBy(PIPELINE)
// TODO this should go to yaml commons
public interface SpecParameters {
  @JsonIgnore
  default SpecParameters getViewJsonObject() {
    return this;
  }
}
