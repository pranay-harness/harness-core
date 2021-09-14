/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import com.hazelcast.spi.exception.TargetNotMemberException;

public class DelegateServiceBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(TargetNotMemberException.class, 2002);
    kryo.register(DelegateScope.class, 73982);
    kryo.register(DelegateSelectionLogParams.class, 73983);
  }
}
