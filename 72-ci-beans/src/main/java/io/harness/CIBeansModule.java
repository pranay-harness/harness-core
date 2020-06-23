package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;

import graph.GraphOperations;
import io.harness.beans.seriazlier.ExecutionProtobufSerializer;
import io.harness.beans.seriazlier.ProtobufSerializer;
import io.harness.beans.steps.CIStepInfo;
import io.harness.govern.DependencyModule;
import io.harness.yaml.core.Execution;

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
    bind(new TypeLiteral<ProtobufSerializer<Execution>>() {}).toInstance(new ExecutionProtobufSerializer());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of();
  }
}
