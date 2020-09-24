package io.harness.walktree.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LevelNode {
  private String qualifierName;
  private LevelNodeType levelNodeType;
}
