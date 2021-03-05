package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
public final class CustomEnvFilterYaml extends EnvironmentFilterYaml {
  private List<String> environments;

  @Builder
  public CustomEnvFilterYaml(@JsonProperty("environments") List<String> environments,
      @JsonProperty("filterType") EnvironmentFilter.EnvironmentFilterType environmentFilterType) {
    super(environmentFilterType);
    setEnvironments(environments);
  }

  public CustomEnvFilterYaml() {}
}
