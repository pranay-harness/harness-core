package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllAppFilter extends ApplicationFilter {
  @JsonCreator
  @Builder
  public AllAppFilter(@JsonProperty("filterType") BlackoutWindowFilterType blackoutWindowFilterType,
      @JsonProperty("envSelection") EnvironmentFilter envSelection) {
    super(blackoutWindowFilterType, envSelection);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("ALL")
  public static final class Yaml extends ApplicationFilterYaml {
    @Builder
    public Yaml(BlackoutWindowFilterType filterType, List<EnvironmentFilterYaml> envSelection) {
      super(filterType, envSelection);
    }

    public Yaml() {}
  }
}
