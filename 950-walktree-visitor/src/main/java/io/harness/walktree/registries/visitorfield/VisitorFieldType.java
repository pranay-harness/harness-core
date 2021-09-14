/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.registries.visitorfield;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class VisitorFieldType {
  @NonNull String type;
}
