package io.harness.grpc.pingpong;

import io.harness.event.PingPongServiceGrpc;
import io.harness.event.PingPongServiceGrpc.PingPongServiceBlockingStub;
import io.harness.govern.ProviderModule;
import io.harness.version.VersionInfoManager;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.CallCredentials;
import io.grpc.Channel;

public class PingPongModule extends ProviderModule {
  @Provides
  @Singleton
  PingPongServiceBlockingStub pingPongServiceBlockingStub(
      @Named("manager-channel") Channel channel, CallCredentials callCredentials) {
    return PingPongServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Provides
  @Singleton
  PingPongClient pingPongClient(
      PingPongServiceBlockingStub pingPongServiceBlockingStub, VersionInfoManager versionInfoManager) {
    return new PingPongClient(pingPongServiceBlockingStub, versionInfoManager.getVersionInfo().getVersion());
  }
}
