/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
