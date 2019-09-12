package io.harness.perpetualtask;

import com.google.inject.Inject;

import java.util.Map;

public class PerpetualTaskServiceClientRegistry {
  @Inject private Map<PerpetualTaskType, PerpetualTaskServiceClient> clientMap;

  public PerpetualTaskServiceClient getClient(PerpetualTaskType type) {
    return clientMap.get(type);
  };
}
