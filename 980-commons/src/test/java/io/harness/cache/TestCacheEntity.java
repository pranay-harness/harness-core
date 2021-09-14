/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cache;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.io.ObjectStreamClass;
import java.util.List;

@OwnedBy(HarnessTeam.PL)
class TestCacheEntity implements Distributable, Nominal {
  public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(TestCacheEntity.class).getSerialVersionUID();

  @Override
  public long structureHash() {
    return STRUCTURE_HASH;
  }

  @Override
  public long algorithmId() {
    return 0;
  }

  @Override
  public long contextHash() {
    return 0;
  }

  @Override
  public String key() {
    return null;
  }

  @Override
  public List<String> parameters() {
    return null;
  }
}
