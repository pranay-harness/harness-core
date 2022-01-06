/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
