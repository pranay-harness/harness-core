package io.harness.walktree.beans;

import io.harness.walktree.visitor.LevelNodeType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LevelNode {
  private String qualifierName;
  private LevelNodeType levelNodeType;
}
