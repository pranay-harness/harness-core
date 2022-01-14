package io.harness.delegate.beans.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
public class FirstCollectionOnDelegate {
  boolean firstCollectionOnDelegate;

  public FirstCollectionOnDelegate(boolean firstCollectionOnDelegate) {
    this.firstCollectionOnDelegate = firstCollectionOnDelegate;
  }
}
