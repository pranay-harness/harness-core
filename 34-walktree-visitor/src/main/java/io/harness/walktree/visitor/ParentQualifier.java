package io.harness.walktree.visitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.walktree.beans.LevelNode;

public interface ParentQualifier { @JsonIgnore LevelNode getLevelNode(); }
