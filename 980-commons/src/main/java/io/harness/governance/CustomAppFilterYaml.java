package io.harness.governance;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
public final class CustomAppFilterYaml extends ApplicationFilterYaml {
  private List<String> apps;

  @Builder
  public CustomAppFilterYaml(
      BlackoutWindowFilterType filterType, List<EnvironmentFilterYaml> envSelection, List<String> apps) {
    super(filterType, envSelection);
    setApps(apps);
  }

  public CustomAppFilterYaml() {}
}
