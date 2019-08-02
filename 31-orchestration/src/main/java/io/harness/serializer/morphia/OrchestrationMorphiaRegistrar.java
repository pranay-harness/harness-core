package io.harness.serializer.morphia;

import io.harness.mongo.MorphiaRegistrar;
import io.harness.state.inspection.ExpressionVariableUsage;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.ListNotifyResponseData;

import java.util.Map;

public class OrchestrationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(pkgHarness + name, clazz);
    };

    h.put("waiter.ListNotifyResponseData", ListNotifyResponseData.class);
    h.put("waiter.ErrorNotifyResponseData", ErrorNotifyResponseData.class);
    h.put("state.inspection.ExpressionVariableUsage", ExpressionVariableUsage.class);
  }
}
