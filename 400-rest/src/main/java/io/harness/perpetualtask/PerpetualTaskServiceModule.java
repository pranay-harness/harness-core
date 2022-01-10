/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.perpetualtask;

import io.harness.perpetualtask.example.SamplePTaskService;
import io.harness.perpetualtask.example.SamplePTaskServiceImpl;

import com.google.inject.AbstractModule;

public class PerpetualTaskServiceModule extends AbstractModule {
  private static volatile PerpetualTaskServiceModule instance;

  public static PerpetualTaskServiceModule getInstance() {
    if (instance == null) {
      instance = new PerpetualTaskServiceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(PerpetualTaskService.class).to(PerpetualTaskServiceImpl.class);
    bind(SamplePTaskService.class).to(SamplePTaskServiceImpl.class);
  }
}
