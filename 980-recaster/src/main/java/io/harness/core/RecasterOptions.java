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
