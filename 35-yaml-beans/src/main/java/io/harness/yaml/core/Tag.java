package io.harness.yaml.core;

import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.ParentQualifier;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

/**
 * Base class for tag structure
 */
@Value
@Builder
public class Tag implements ParentQualifier {
  @NotNull String key;
  @NotNull String value;

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName("tag").build();
  }
}
