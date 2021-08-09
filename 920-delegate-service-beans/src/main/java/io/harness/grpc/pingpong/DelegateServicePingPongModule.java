package io.harness.grpc.pingpong;

import io.harness.govern.ProviderModule;
import io.harness.pingpong.DelegateServicePingPongGrpc;
import io.harness.version.VersionInfoManager;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class DelegateServicePingPongModule extends ProviderModule {
  @Provides
  @Singleton
  DelegateServicePingPongClient delegateServicePingPongClient(
      DelegateServicePingPongGrpc.DelegateServicePingPongBlockingStub pingPongServiceBlockingStub,
      VersionInfoManager versionInfoManager) {
    return new DelegateServicePingPongClient(
        pingPongServiceBlockingStub, versionInfoManager.getVersionInfo().getVersion());
  }
}
