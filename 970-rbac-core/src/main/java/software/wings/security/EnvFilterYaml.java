package software.wings.security;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnvFilterYaml extends FilterYaml {
  private List<String> filterTypes;

  @Builder
  public EnvFilterYaml(List<String> entityNames, List<String> filterTypes) {
    super(entityNames);
    this.filterTypes = filterTypes;
  }
}
