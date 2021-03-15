package io.harness.governance;

import static io.harness.governance.EnvironmentFilter.EnvironmentFilterType;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ALL")
public final class AllEnvFilterYaml extends EnvironmentFilterYaml {
  @Builder
  public AllEnvFilterYaml(@JsonProperty("filterType") EnvironmentFilterType environmentFilterType) {
    super(environmentFilterType);
  }

  public AllEnvFilterYaml() {}
}
