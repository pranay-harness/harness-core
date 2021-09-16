package io.harness.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonTypeName("CUSTOM")
@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDC)
public class CustomEnvFilter extends EnvironmentFilter {
  private List<String> environments;
  @JsonCreator
  @Builder
  public CustomEnvFilter(@JsonProperty("filterType") EnvironmentFilterType filterType,
      @JsonProperty("environments") List<String> environments) {
    super(filterType);
    this.environments = environments;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("CUSTOM")
  public static final class Yaml extends EnvironmentFilterYaml {
    private List<String> environments;

    @Builder
    public Yaml(@JsonProperty("environments") List<String> environments,
        @JsonProperty("filterType") EnvironmentFilterType environmentFilterType) {
      super(environmentFilterType);
      setEnvironments(environments);
    }

    public Yaml() {}
  }
}