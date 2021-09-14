/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class PartialPlanResponseKryoSerializer extends ProtobufKryoSerializer<PartialPlanResponse> {
  private static PartialPlanResponseKryoSerializer instance;

  private PartialPlanResponseKryoSerializer() {}

  public static synchronized PartialPlanResponseKryoSerializer getInstance() {
    if (instance == null) {
      instance = new PartialPlanResponseKryoSerializer();
    }
    return instance;
  }
}
