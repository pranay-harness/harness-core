package io.harness.delegate.beans.connector;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;

import java.util.List;
import java.util.Set;

public class ConnectorCapabilityBaseHelper {
  public static void populateDelegateSelectorCapability(
      List<ExecutionCapability> capabilityList, Set<String> delegateSelectors) {
    if (hasSome(delegateSelectors)) {
      capabilityList.add(SelectorCapability.builder().selectors(delegateSelectors).build());
    }
  }
}
