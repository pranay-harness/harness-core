package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonTypeName("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDC)
public class AllEnvFilter extends EnvironmentFilter {
  @Builder
  @JsonCreator
  public AllEnvFilter(@JsonProperty("filterType") EnvironmentFilterType filterType) {
    super(filterType);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("ALL")
  public static final class Yaml extends EnvironmentFilterYaml {
    @Builder
    public Yaml(@JsonProperty("filterType") EnvironmentFilterType environmentFilterType) {
      super(environmentFilterType);
    }

    public Yaml() {}
  }
}
