package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ALL_NON_PROD")
public final class AllNonProdEnvFilterYaml extends EnvironmentFilterYaml {
  @Builder
  public AllNonProdEnvFilterYaml(
      @JsonProperty("filterType") EnvironmentFilter.EnvironmentFilterType environmentFilterType) {
    super(environmentFilterType);
  }

  public AllNonProdEnvFilterYaml() {}
}
