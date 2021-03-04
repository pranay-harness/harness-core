package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ALL")
public final class AllEnvFilterYaml extends EnvironmentFilterYaml {
  @Builder
  public AllEnvFilterYaml(@JsonProperty("filterType") EnvironmentFilter.EnvironmentFilterType environmentFilterType) {
    super(environmentFilterType);
  }

  public AllEnvFilterYaml() {}
}
