package io.harness.service.intfc;

import io.harness.delegate.beans.Delegate;

public interface DelegateCache {
  Delegate get(String accountId, String delegateId, boolean forceRefresh);
}
