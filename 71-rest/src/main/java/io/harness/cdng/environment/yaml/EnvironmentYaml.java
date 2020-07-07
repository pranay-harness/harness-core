package io.harness.cdng.environment.yaml;

import io.harness.cdng.common.beans.Tag;
import io.harness.cdng.environment.beans.EnvironmentType;
import io.harness.data.Outcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.yaml.core.intfc.OverridesApplier;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;

import java.util.List;

@Value
@Builder
public class EnvironmentYaml implements Outcome, OverridesApplier<EnvironmentYaml> {
  @NonFinal @Wither String displayName;
  @Wither String identifier;
  @Wither EnvironmentType type;
  @Wither List<Tag> tags;

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public EnvironmentYaml applyOverrides(EnvironmentYaml overrideConfig) {
    EnvironmentYaml resultant = this;
    if (EmptyPredicate.isNotEmpty(overrideConfig.getDisplayName())) {
      resultant = resultant.withDisplayName(overrideConfig.getDisplayName());
    }
    if (EmptyPredicate.isNotEmpty(identifier)) {
      resultant = resultant.withIdentifier(overrideConfig.getIdentifier());
    }
    if (overrideConfig.getType() != null) {
      resultant = resultant.withType(overrideConfig.getType());
    }
    if (EmptyPredicate.isNotEmpty(overrideConfig.getTags())) {
      resultant = resultant.withTags(overrideConfig.getTags());
    }
    return resultant;
  }
}
