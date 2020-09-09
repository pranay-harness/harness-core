package io.harness.cdng.variables;

import io.harness.cdng.visitor.helpers.deploymentstage.StageVariablesVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.PreviousStageAware;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder
@SimpleVisitorHelper(helperClass = StageVariablesVisitorHelper.class)
public class StageVariables implements PreviousStageAware, Visitable {
  @Singular private List<Variable> variables;
  private String previousStageIdentifier;
  @Singular private List<Variable> overrides;

  @Override
  public List<Object> getChildrenToWalk() {
    // this will change once variables are properly implemented
    return variables.stream().map(stage -> (Object) stage).collect(Collectors.toList());
  }
}
