package software.wings.security;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GenericEntityFilterYaml extends FilterYaml {
  private String filterType;

  @Builder
  public GenericEntityFilterYaml(List<String> names, String filterType) {
    super(names);
    this.filterType = filterType;
  }
}
