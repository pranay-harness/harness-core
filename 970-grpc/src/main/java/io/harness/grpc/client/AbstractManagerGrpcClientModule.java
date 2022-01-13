/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.client;

import io.harness.govern.ProviderModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public abstract class AbstractManagerGrpcClientModule extends ProviderModule {
  @Override
  protected void configure() {
    install(ManagerGrpcClientModule.getInstance());
  }

  @Provides
  @Singleton
  protected ManagerGrpcClientModule.Config injectConfig() {
    return config();
  }

  public abstract ManagerGrpcClientModule.Config config();

  @Provides
  @Singleton
  @Named("Application")
  protected String injectApplication() {
    return application();
  }

  public abstract String application();
}
