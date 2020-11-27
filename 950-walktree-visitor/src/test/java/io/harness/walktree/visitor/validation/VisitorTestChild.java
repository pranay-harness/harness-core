package io.harness.walktree.visitor.validation;

import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.ParentQualifier;
import io.harness.walktree.visitor.validation.annotations.Required;
import io.harness.walktree.visitor.validation.modes.PreInputSet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitorTestChild implements ParentQualifier {
  @Required(groups = PreInputSet.class) private String name;

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName("VisitorTestChild").build();
  }
}
