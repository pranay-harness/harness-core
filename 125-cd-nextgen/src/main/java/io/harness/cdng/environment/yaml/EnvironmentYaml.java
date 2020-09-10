package io.harness.cdng.environment.yaml;

import io.harness.cdng.visitor.helpers.pipelineinfrastructure.EnvironmentYamlVisitorHelper;
import io.harness.data.Outcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.common.beans.Tag;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverridesApplier;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;

import java.util.List;

@Value
@Builder
@SimpleVisitorHelper(helperClass = EnvironmentYamlVisitorHelper.class)
public class EnvironmentYaml implements Outcome, OverridesApplier<EnvironmentYaml>, Visitable {
  @NonFinal @Wither String name;
  @Wither String identifier;
  @Wither EnvironmentType type;
  @Wither List<Tag> tags;

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public EnvironmentYaml applyOverrides(EnvironmentYaml overrideConfig) {
    EnvironmentYaml resultant = this;
    if (EmptyPredicate.isNotEmpty(overrideConfig.getName())) {
      resultant = resultant.withName(overrideConfig.getName());
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

  @Override
  public VisitableChildren getChildrenToWalk() {
    // returning empty list for now
    return VisitableChildren.builder().build();
  }
}
