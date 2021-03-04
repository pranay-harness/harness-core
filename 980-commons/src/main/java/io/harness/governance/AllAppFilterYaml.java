package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
