package io.harness.governance;

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
@JsonTypeName("ALL_PROD")
public final class AllProdEnvFilterYaml extends EnvironmentFilterYaml {
  @Builder
  public AllProdEnvFilterYaml(
      @JsonProperty("filterType") EnvironmentFilter.EnvironmentFilterType environmentFilterType) {
    super(environmentFilterType);
  }

  public AllProdEnvFilterYaml() {}
}
