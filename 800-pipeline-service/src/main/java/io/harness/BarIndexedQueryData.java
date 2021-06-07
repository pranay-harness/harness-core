package io.harness;

import io.harness.persistence.IndexedQueryData;

public class BarIndexedQueryData implements IndexedQueryData {
  public String getCanonicalForm() {
    return "collection(DelegateTask).filter(_id in list<+>, accountId = <+>, status = <+>).project(_id, dataTimeout)";
  }
}
