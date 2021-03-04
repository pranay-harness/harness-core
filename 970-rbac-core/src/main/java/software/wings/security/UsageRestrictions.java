package software.wings.security;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsageRestrictions {
  private Set<AppEnvRestriction> appEnvRestrictions;

  @Data
  @Builder
  @EqualsAndHashCode
  public static class AppEnvRestriction {
    private GenericEntityFilter appFilter;
    private EnvFilter envFilter;
  }
}
