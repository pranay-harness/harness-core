package software.wings.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.yaml.BaseYaml;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class UsageRestrictions {
  private Set<AppEnvRestriction> appEnvRestrictions;

  @Data
  @Builder
  @EqualsAndHashCode
  public static class AppEnvRestriction {
    private GenericEntityFilter appFilter;
    private EnvFilter envFilter;

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Yaml extends BaseYaml {
      private GenericEntityFilter.Yaml appFilter;
      private EnvFilter.Yaml envFilter;

      @Builder
      public Yaml(GenericEntityFilter.Yaml appFilter, EnvFilter.Yaml envFilter) {
        this.appFilter = appFilter;
        this.envFilter = envFilter;
      }
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends BaseYaml {
    private List<AppEnvRestriction.Yaml> appEnvRestrictions;

    @Builder
    public Yaml(List<AppEnvRestriction.Yaml> appEnvRestrictions) {
      this.appEnvRestrictions = appEnvRestrictions;
    }
  }
}
