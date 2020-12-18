package io.harness.pms.sdk.core.recast;

import lombok.Data;

@Data
public class RecasterOptions {
  private boolean ignoreFinalFields;
  private boolean storeNulls;
  private boolean storeEmpties;
}
