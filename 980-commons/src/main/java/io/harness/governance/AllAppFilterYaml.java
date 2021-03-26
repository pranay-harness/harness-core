package io.harness.governance;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ALL")
public final class AllAppFilterYaml extends ApplicationFilterYaml {
  @Builder
  public AllAppFilterYaml(BlackoutWindowFilterType filterType, List<EnvironmentFilterYaml> envSelection) {
    super(filterType, envSelection);
  }

  public AllAppFilterYaml() {}
}
