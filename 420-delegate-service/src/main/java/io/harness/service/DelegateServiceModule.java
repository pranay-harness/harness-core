package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ff.FeatureFlagModule;
import io.harness.service.impl.DelegateCacheImpl;
import io.harness.service.impl.DelegateCallbackRegistryImpl;
import io.harness.service.impl.DelegateInsightsServiceImpl;
import io.harness.service.impl.DelegateSetupServiceImpl;
import io.harness.service.impl.DelegateTaskSelectorMapServiceImpl;
import io.harness.service.impl.DelegateTaskServiceImpl;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.service.intfc.DelegateInsightsService;
import io.harness.service.intfc.DelegateSetupService;
import io.harness.service.intfc.DelegateTaskSelectorMapService;
import io.harness.service.intfc.DelegateTaskService;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(FeatureFlagModule.getInstance());

    bind(DelegateTaskService.class).to(DelegateTaskServiceImpl.class);
    bind(DelegateCallbackRegistry.class).to(DelegateCallbackRegistryImpl.class);
    bind(DelegateTaskSelectorMapService.class).to(DelegateTaskSelectorMapServiceImpl.class);
    bind(DelegateInsightsService.class).to(DelegateInsightsServiceImpl.class);
    bind(DelegateCache.class).to(DelegateCacheImpl.class);
    bind(DelegateSetupService.class).to(DelegateSetupServiceImpl.class);
  }
}
