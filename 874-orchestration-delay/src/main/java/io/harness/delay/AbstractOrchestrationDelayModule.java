/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delay;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class AbstractOrchestrationDelayModule extends AbstractModule {
  @Override
  protected void configure() {
    install(OrchestrationDelayModule.getInstance());
  }

  @Provides
  @Singleton
  @Named("forNG")
  protected boolean forNGProvider() {
    return forNG();
  }

  public abstract boolean forNG();
}
