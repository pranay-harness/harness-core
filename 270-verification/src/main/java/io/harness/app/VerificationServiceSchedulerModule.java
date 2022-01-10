/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.app;

import io.harness.scheduler.PersistentScheduler;
import io.harness.scheduler.VerificationJobScheduler;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * @author Raghu
 */
public class VerificationServiceSchedulerModule extends AbstractModule {
  private final VerificationServiceConfiguration configuration;

  public VerificationServiceSchedulerModule(VerificationServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(PersistentScheduler.class)
        .annotatedWith(Names.named("BackgroundJobScheduler"))
        .to(VerificationJobScheduler.class)
        .asEagerSingleton();
  }
}
