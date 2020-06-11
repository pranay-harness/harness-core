package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;

import graph.GraphOperations;
import io.harness.beans.steps.CIStepInfo;
import io.harness.govern.DependencyModule;

import java.util.Set;

public class CIBeansModule extends DependencyModule {
  private static CIBeansModule instance;

  public static CIBeansModule getInstance() {
    if (instance == null) {
      instance = new CIBeansModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<GraphOperations<CIStepInfo>>() {}).toInstance(new GraphOperations<>());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of();
  }
}
