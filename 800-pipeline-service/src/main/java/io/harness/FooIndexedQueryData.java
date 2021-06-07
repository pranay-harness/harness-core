package io.harness;

import io.harness.persistence.IndexedQueryData;

public class FooIndexedQueryData implements IndexedQueryData {
  public String getCanonicalForm() {
    return "collection(DelegateTask).filter(_id in list<+>, accountId = <+>, delegateId = <+>, status = <+>).project(_id, dataTimeout)";
  }
}
