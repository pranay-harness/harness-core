package io.harness.connector;

import java.util.Set;

public interface DelegateSelectable {
  Set<String> getDelegateSelectors();
  void setDelegateSelectors(Set<String> delegateSelectors);
}
