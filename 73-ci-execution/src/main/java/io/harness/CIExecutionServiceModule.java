package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.govern.DependencyModule;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.impl.CIPipelineExecutionServiceImpl;
import io.harness.registries.RegistryModule;

import java.util.Set;

public class CIExecutionServiceModule extends DependencyModule {
  private static CIExecutionServiceModule instance;
  public static CIExecutionServiceModule getInstance() {
    if (instance == null) {
      instance = new CIExecutionServiceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(CIPipelineExecutionService.class).to(CIPipelineExecutionServiceImpl.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of(RegistryModule.getInstance());
  }
}