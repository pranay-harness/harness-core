package software.wings.security;

import io.harness.yaml.BaseYaml;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FilterYaml extends BaseYaml {
  private List<String> entityNames;

  public FilterYaml(List<String> entityNames) {
    this.entityNames = entityNames;
  }
}
