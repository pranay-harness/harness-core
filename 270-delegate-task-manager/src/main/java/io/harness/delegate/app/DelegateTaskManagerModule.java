package io.harness.delegate.app;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.service.DelegateService;
import io.harness.delegate.service.DelegateServiceImpl;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateTaskManagerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DelegateService.class).to(DelegateServiceImpl.class);
  }
}
