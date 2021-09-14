/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;

import java.util.List;
import java.util.Set;

public class ConnectorCapabilityBaseHelper {
  public static void populateDelegateSelectorCapability(
      List<ExecutionCapability> capabilityList, Set<String> delegateSelectors) {
    if (isNotEmpty(delegateSelectors)) {
      capabilityList.add(SelectorCapability.builder().selectors(delegateSelectors).build());
    }
  }
}
