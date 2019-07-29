package io.harness.serializer.morphia;

import io.harness.mongo.MorphiaRegistrar;
import io.harness.waiter.ListNotifyResponseData;

import java.util.Map;

public class OrchestrationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Map<String, Class> map) {
    map.put(harnessPackage + "waiter.ListNotifyResponseData", ListNotifyResponseData.class);
  }
}
