/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cache;

import io.harness.PersistenceTestBase;

import java.io.ObjectStreamClass;
import java.util.List;
import lombok.Builder;
import lombok.Value;

public class MongoStoreTestBase extends PersistenceTestBase {
  @Value
  @Builder
  public static class TestNominalEntity implements Distributable, Nominal {
    public static final long STRUCTURE_HASH =
        ObjectStreamClass.lookup(MongoStoreTestBase.TestNominalEntity.class).getSerialVersionUID();
    public static final long algorithmId = 0;

    private long contextHash;

    private String key;
    private String value;

    @Override
    public long structureHash() {
      return STRUCTURE_HASH;
    }

    @Override
    public long algorithmId() {
      return algorithmId;
    }

    @Override
    public long contextHash() {
      return contextHash;
    }

    @Override
    public String key() {
      return key;
    }

    @Override
    public List<String> parameters() {
      return null;
    }
  }

  @Value
  @Builder
  public static class TestOrdinalEntity implements Distributable, Ordinal {
    public static final long STRUCTURE_HASH =
        ObjectStreamClass.lookup(MongoStoreTestBase.TestOrdinalEntity.class).getSerialVersionUID();
    public static final long algorithmId = 0;

    protected long contextOrder;

    private String key;
    private String value;

    @Override
    public long structureHash() {
      return STRUCTURE_HASH;
    }

    @Override
    public long algorithmId() {
      return algorithmId;
    }

    @Override
    public long contextOrder() {
      return contextOrder;
    }

    @Override
    public String key() {
      return key;
    }

    @Override
    public List<String> parameters() {
      return null;
    }
  }
}
