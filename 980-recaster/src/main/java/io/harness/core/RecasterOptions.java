/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.core;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecasterOptions {
  boolean ignoreFinalFields;
  boolean storeNulls;
  boolean storeEmpties;

  // to be removed when migration to map is finished
  boolean workWithMaps;
}
