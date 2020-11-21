package io.harness.capability.service;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public interface CapabilityService {
  // returns all the valid delegates that can execute all the required capabilities according to
  // the indexed capability results
  Set<String> fetchValidDelegates(String accountId, Set<ExecutionCapability> requiredCapabilities);

  // returns the list of all capabilities under this account
  Set<ExecutionCapability> fetchCapabilitySet(String accountId);

  // returns the list of all valid capabilities for a given delegate
  Set<ExecutionCapability> fetchCapabilitiesForDelegate(String accountId, String delegateId);

  // add capabilities to the account's registry, and then broadcast to all the delegates to evaluate
  // and return the capability
  void addCapabilities(String accountId, Set<ExecutionCapability> capabilities);

  // remove capabilities from the account's registry, and remove the evaluation result from the
  // database. If the capability is to be used again, it will need to be added.
  void removeCapabilities(String accountId, Set<ExecutionCapability> capabilities);

  // invalidate delegate-capability pairs due to failure of capability usage. If requested, we can
  // mark the capability as invalid, and revalidate them after they have had the time to cool off.
  void invalidateCapabilities(String accountId, Set<Pair<String /* delegateId */, ExecutionCapability>> capabilityPairs,
      boolean shouldReevaluate);

  // update valid capabilities as valid, due to success in capability usage. Do not use with
  // delegate-capability pairs that are not currently valid.
  void confirmCapabilities(String accountId, Set<Pair<String /* delegateId */, ExecutionCapability>> capabilityPairs);

  // take all capabilities that need to be re-evaluated as of now and broadcast revalidation to
  // the appropriate delegates.
  void reevaluateStaleCapabilities(String accountId);

  // returns the time at which there are capabilities that should be re-evaluated.
  long fetchRefreshTime(String accountId);
}
