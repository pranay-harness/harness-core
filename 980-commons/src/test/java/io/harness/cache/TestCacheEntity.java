/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
