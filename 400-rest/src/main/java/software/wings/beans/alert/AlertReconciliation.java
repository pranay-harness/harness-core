/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "AlertReconciliationKeys")
@TargetModule(HarnessModule._955_ALERT_BEANS)
public class AlertReconciliation {
  @Getter private boolean needed;
  @Getter @Setter private Long nextIteration;

  public static final AlertReconciliation noop = new AlertReconciliation();
}
