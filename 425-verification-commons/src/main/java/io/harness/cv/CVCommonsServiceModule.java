/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
