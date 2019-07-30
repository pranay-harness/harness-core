package io.harness.serializer.morphia;

import io.harness.mongo.MorphiaRegistrar;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.ListNotifyResponseData;

import java.util.Map;

public class OrchestrationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Map<String, Class> map) {
    map.put(pkgHarness + "waiter.ListNotifyResponseData", ListNotifyResponseData.class);
    map.put(pkgHarness + "waiter.ErrorNotifyResponseData", ErrorNotifyResponseData.class);
  }
}
