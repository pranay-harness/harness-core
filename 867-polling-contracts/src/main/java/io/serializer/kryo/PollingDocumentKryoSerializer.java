/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.contracts.service.PollingDocument;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(PIPELINE)
public class PollingDocumentKryoSerializer extends ProtobufKryoSerializer<PollingDocument> {
  private static PollingDocumentKryoSerializer instance;

  public PollingDocumentKryoSerializer() {}

  public static synchronized PollingDocumentKryoSerializer getInstance() {
    if (instance == null) {
      instance = new PollingDocumentKryoSerializer();
    }
    return instance;
  }
}
