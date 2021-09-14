/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitableChild {
  String fieldName;
  Object value;
}
