package io.harness.core;

import lombok.Data;

@Data
public class RecasterOptions {
  private boolean ignoreFinalFields;
  private boolean storeNulls;
  private boolean storeEmpties;
}
