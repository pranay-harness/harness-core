/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.context;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalContext {
  private Map<String, GlobalContextData> map = new HashMap<>();

  public GlobalContext() {}

  public GlobalContext(GlobalContext globalContext) {
    map = globalContext == null || globalContext.map == null ? new HashMap<>() : new HashMap<>(globalContext.map);
  }

  public <T extends GlobalContextData> T get(String key) {
    GlobalContextData globalContextData = map.get(key);
    if (globalContextData != null && !key.equals(globalContextData.getKey())) {
      throw new RuntimeException("This should never happen");
    }
    return (T) globalContextData;
  }

  public void setGlobalContextRecord(GlobalContextData data) {
    if (map.containsKey(data.getKey())) {
      log.error("Global data {} is already set. Something is wrong!!!", data.getKey(), new Exception());
    }
    map.put(data.getKey(), data);
  }

  public void upsertGlobalContextRecord(GlobalContextData data) {
    map.put(data.getKey(), data);
  }

  public void unset(String key) {
    map.remove(key);
  }
}
