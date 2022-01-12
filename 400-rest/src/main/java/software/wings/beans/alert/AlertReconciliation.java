/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
