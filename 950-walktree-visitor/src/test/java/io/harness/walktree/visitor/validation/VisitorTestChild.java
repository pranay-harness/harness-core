/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.validation;

import io.harness.walktree.visitor.validation.annotations.Required;
import io.harness.walktree.visitor.validation.modes.PreInputSet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitorTestChild {
  @Required(groups = PreInputSet.class) private String name;
}
