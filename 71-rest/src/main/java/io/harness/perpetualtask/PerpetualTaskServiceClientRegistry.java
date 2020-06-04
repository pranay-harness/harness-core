package io.harness.perpetualtask;

import com.google.inject.Inject;

import java.util.Map;

public class PerpetualTaskServiceClientRegistry {
  @Inject private Map<PerpetualTaskType, PerpetualTaskServiceClient> clientMap;

  public PerpetualTaskServiceClient getClient(PerpetualTaskType type) {
    return clientMap.get(type);
  }

  public PerpetualTaskServiceInprocClient getInprocClient(PerpetualTaskType type) {
    PerpetualTaskServiceClient perpetualTaskServiceClient = clientMap.get(type);
    if (perpetualTaskServiceClient instanceof PerpetualTaskServiceInprocClient) {
      return (PerpetualTaskServiceInprocClient) perpetualTaskServiceClient;
    }
    return null;
  }
}
