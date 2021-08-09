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
