/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
