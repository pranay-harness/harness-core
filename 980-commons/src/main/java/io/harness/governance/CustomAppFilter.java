package io.harness.governance;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(NON_NULL)
public class CustomAppFilter extends ApplicationFilter {
  private List<String> apps;

  @JsonCreator
  @Builder
  public CustomAppFilter(@JsonProperty("filterType") BlackoutWindowFilterType blackoutWindowFilterType,
      @JsonProperty("envSelection") EnvironmentFilter envSelection, @JsonProperty("apps") List<String> apps,
      @JsonProperty("serviceSelection") ServiceFilter serviceSelection) {
    super(blackoutWindowFilterType, envSelection, serviceSelection);
    this.apps = apps;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("CUSTOM")
  public static final class Yaml extends ApplicationFilterYaml {
    private List<String> apps;

    @Builder
    public Yaml(BlackoutWindowFilterType filterType, List<EnvironmentFilterYaml> envSelection, List<String> apps,
        List<ServiceFilter.Yaml> serviceSelection) {
      super(filterType, envSelection, serviceSelection);
      setApps(apps);
    }

    public Yaml() {}
  }
}
