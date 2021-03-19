package io.harness.governance;

import static io.harness.governance.EnvironmentFilter.EnvironmentFilterType;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
public final class CustomEnvFilterYaml extends EnvironmentFilterYaml {
  private List<String> environments;

  @Builder
  public CustomEnvFilterYaml(@JsonProperty("environments") List<String> environments,
      @JsonProperty("filterType") EnvironmentFilterType environmentFilterType) {
    super(environmentFilterType);
    setEnvironments(environments);
  }

  public CustomEnvFilterYaml() {}
}
