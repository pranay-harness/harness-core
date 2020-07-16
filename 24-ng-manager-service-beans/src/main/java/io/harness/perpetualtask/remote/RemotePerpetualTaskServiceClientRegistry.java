package io.harness.perpetualtask.remote;

import com.google.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RemotePerpetualTaskServiceClientRegistry {
  private final Map<String, RemotePerpetualTaskServiceClient> clientMap = new ConcurrentHashMap<>();

  public void registerClient(String type, RemotePerpetualTaskServiceClient client) {
    clientMap.putIfAbsent(type, client);
  }

  public Optional<RemotePerpetualTaskServiceClient> getClient(String type) {
    return Optional.ofNullable(clientMap.get(type));
  }
}
