package io.harness.yaml.extended.ci.codebase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Build {
  @NotNull private BuildType type;
  @NotNull private BuildSpec spec;
}
