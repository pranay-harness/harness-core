/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class ErrorResponseKryoSerializer extends ProtobufKryoSerializer<ErrorResponse> {
  private static ErrorResponseKryoSerializer instance;

  private ErrorResponseKryoSerializer() {}

  public static synchronized ErrorResponseKryoSerializer getInstance() {
    if (instance == null) {
      instance = new ErrorResponseKryoSerializer();
    }
    return instance;
  }
}
