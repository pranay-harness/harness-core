/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cv;

import io.harness.cv.api.WorkflowVerificationResultService;
import io.harness.cv.impl.WorkflowVerificationResultServiceImpl;

import com.google.inject.AbstractModule;

public class CVCommonsServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(WorkflowVerificationResultService.class).to(WorkflowVerificationResultServiceImpl.class);
  }
}
