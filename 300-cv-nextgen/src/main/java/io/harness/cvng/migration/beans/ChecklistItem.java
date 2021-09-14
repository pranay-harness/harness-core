/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.migration.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChecklistItem {
  public static ChecklistItem NA = ChecklistItem.builder().desc("N/A").build();
  String desc;
}
