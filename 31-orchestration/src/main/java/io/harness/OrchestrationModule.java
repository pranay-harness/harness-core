package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.govern.DependencyModule;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
import io.harness.waiter.WaiterModule;

import java.util.Set;

public class OrchestrationModule extends DependencyModule {
  private static OrchestrationModule instance;

  public static OrchestrationModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(StateInspectionService.class).to(StateInspectionServiceImpl.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(WaiterModule.getInstance());
  }
}
