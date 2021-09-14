/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class PerpetualTaskServiceClientRegistry {
  private final Map<String, PerpetualTaskServiceClient> clientMap = new HashMap<>();

  public void registerClient(String type, PerpetualTaskServiceClient client) {
    clientMap.putIfAbsent(type, client);
  }

  public PerpetualTaskServiceClient getClient(String type) {
    return clientMap.get(type);
  }
}
