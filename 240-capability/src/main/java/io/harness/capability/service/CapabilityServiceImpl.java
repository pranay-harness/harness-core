package io.harness.capability.service;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.exception.GeneralException;

import software.wings.beans.Delegate;
import software.wings.service.impl.DelegateObserver;

import com.google.inject.Singleton;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Slf4j
public class CapabilityServiceImpl implements CapabilityService, DelegateObserver {
  @Override
  public Set<String> fetchValidDelegates(String accountid, Set<ExecutionCapability> requiredCapabilities) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public Set<ExecutionCapability> fetchCapabilitySet(String accountId) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public Set<ExecutionCapability> fetchCapabilitiesForDelegate(String accountId, String delegateId) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public void addCapabilities(String accountId, Set<ExecutionCapability> capabilities) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public void removeCapabilities(String accountId, Set<ExecutionCapability> capabilities) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public void invalidateCapabilities(String accountId,
      Set<Pair<String /* delegateId */, ExecutionCapability>> capabilityPairs, boolean shouldReevaluate) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public void confirmCapabilities(
      String accountId, Set<Pair<String /* delegateId */, ExecutionCapability>> capabilityPairs) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public void reevaluateStaleCapabilities(String accountId) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public long fetchRefreshTime(String accountId) {
    throw new GeneralException("not implemented yet");
  }

  @Override
  public void onAdded(Delegate delegate) {
    // we should reevaluate all capabilities that are not actively valid on the given delegate
    throw new GeneralException("not implemented yet");
  }

  @Override
  public void onDisconnected(String accountId, String delegateId) {
    // we should set up a timer so that if the delegate doesn't come back online for 15 minutes
    // or so, we will remove the delegate capabilities from the registries.
  }
}
