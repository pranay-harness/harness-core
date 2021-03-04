package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
