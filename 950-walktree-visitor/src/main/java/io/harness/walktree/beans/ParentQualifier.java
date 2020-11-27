package io.harness.walktree.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ParentQualifier {
  @JsonIgnore LevelNode getLevelNode();
}
