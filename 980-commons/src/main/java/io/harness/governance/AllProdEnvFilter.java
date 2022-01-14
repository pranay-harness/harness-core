package io.harness.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ALL_PROD")
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class AllProdEnvFilter extends EnvironmentFilter {
  @Builder
  @JsonCreator
  public AllProdEnvFilter(@JsonProperty("filterType") EnvironmentFilterType filterType) {
    super(filterType);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("ALL_PROD")
  public static final class Yaml extends EnvironmentFilterYaml {
    @Builder
    public Yaml(@JsonProperty("filterType") EnvironmentFilterType environmentFilterType) {
      super(environmentFilterType);
    }

    public Yaml() {}
  }
}
