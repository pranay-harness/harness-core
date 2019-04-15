package io.harness.context;

import static java.lang.String.format;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GlobalContext {
  private Map<String, GlobalContextData> map = new ConcurrentHashMap<>();

  public GlobalContextData get(String key) {
    GlobalContextData globalContextData = map.get(key);
    if (globalContextData != null && !key.equals(globalContextData.getKey())) {
      throw new RuntimeException("This should never happen");
    }

    return globalContextData;
  }

  public void setGlobalContextRecord(GlobalContextData data) {
    if (map.containsKey(data.getKey())) {
      logger.error(format("Global data %s is already set. Something is wrong!!!", data.getKey()), new Exception());
    }
    map.put(data.getKey(), data);
  }

  public void upsertGlobalContextRecord(GlobalContextData data) {
    map.put(data.getKey(), data);
  }
}
