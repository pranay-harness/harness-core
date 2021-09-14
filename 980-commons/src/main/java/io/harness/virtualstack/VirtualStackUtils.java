/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.virtualstack;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.manage.GlobalContextManager;
import io.harness.serializer.KryoSerializer;

import com.google.protobuf.ByteString;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
@Slf4j
public class VirtualStackUtils {
  public VirtualStackRequest populateRequest(KryoSerializer kryoSerializer) {
    VirtualStackRequest.Builder builder = VirtualStackRequest.newBuilder();
    try {
      builder.setGlobalContext(
          ByteString.copyFrom(kryoSerializer.asDeflatedBytes(GlobalContextManager.obtainGlobalContext())));

    } catch (Exception exception) {
      log.error("Make sure deflating the global object is supported", exception);
    }

    return builder.build();
  }
}
