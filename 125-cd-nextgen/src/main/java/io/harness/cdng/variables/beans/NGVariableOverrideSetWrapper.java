package io.harness.cdng.variables.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NGVariableOverrideSetWrapper {
  NGVariableOverrideSets overrideSet;
  String uuid;
}
