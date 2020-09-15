package io.harness.cdng.environment.yaml;

import io.harness.beans.ParameterField;
import io.harness.cdng.visitor.helpers.pipelineinfrastructure.EnvironmentYamlVisitorHelper;
import io.harness.data.Outcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.common.beans.Tag;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverridesApplier;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;

import java.util.List;

@Data
@Builder
@SimpleVisitorHelper(helperClass = EnvironmentYamlVisitorHelper.class)
public class EnvironmentYaml implements Outcome, OverridesApplier<EnvironmentYaml>, Visitable {
  @Wither ParameterField<String> name;
  @Wither ParameterField<String> identifier;
  @Wither EnvironmentType type;
  @Wither List<Tag> tags;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public EnvironmentYaml applyOverrides(EnvironmentYaml overrideConfig) {
    EnvironmentYaml resultant = this;
    if (overrideConfig.getName() != null) {
      resultant = resultant.withName(overrideConfig.getName());
    }
    if (overrideConfig.getIdentifier() != null) {
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
