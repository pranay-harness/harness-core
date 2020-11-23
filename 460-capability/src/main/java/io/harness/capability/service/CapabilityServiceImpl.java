package io.harness.capability.service;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.exception.GeneralException;

import com.google.inject.Singleton;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Slf4j
public class CapabilityServiceImpl implements CapabilityService {
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
}
