/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.validation.modes;

public interface ModeType {
  Class<?> POST_INPUT_SET = PostInputSet.class;
  Class<?> PRE_INPUT_SET = PreInputSet.class;
}
