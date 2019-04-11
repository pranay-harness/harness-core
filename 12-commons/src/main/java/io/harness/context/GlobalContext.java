package io.harness.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalContext {
  private Map<String, GlobalContextData> map = new ConcurrentHashMap<>();

  public GlobalContextData get(String key) {
    GlobalContextData globalContextData = map.get(key);
    if (globalContextData != null && !key.equals(globalContextData.getKey())) {
      throw new RuntimeException("This should never happen");
    }

    return globalContextData;
  }

  public void upsertGlobalContextRecord(GlobalContextData data) {
    map.put(data.getKey(), data);
  }
}
