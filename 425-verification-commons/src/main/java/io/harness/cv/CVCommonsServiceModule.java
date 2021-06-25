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
