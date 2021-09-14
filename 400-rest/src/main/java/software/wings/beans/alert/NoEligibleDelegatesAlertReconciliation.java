/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "NoEligibleDelegatesAlertReconciliationKeys")
@Value
@Builder
public class NoEligibleDelegatesAlertReconciliation extends AlertReconciliation {
  private List<String> delegates;
}
