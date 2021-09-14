/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class RefTypeKryoSerializer extends ProtobufKryoSerializer<RefType> {
  private static RefTypeKryoSerializer instance;

  private RefTypeKryoSerializer() {}

  public static synchronized RefTypeKryoSerializer getInstance() {
    if (instance == null) {
      instance = new RefTypeKryoSerializer();
    }
    return instance;
  }
}
