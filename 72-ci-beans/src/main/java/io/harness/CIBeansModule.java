package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;

import graph.GraphOperations;
import io.harness.beans.serializer.ExecutionProtobufSerializer;
import io.harness.beans.serializer.ProtobufSerializer;
import io.harness.beans.serializer.PublishStepProtobufSerializer;
import io.harness.beans.serializer.RestoreCacheStepProtobufSerializer;
import io.harness.beans.serializer.RunStepProtobufSerializer;
import io.harness.beans.serializer.SaveCacheStepProtobufSerializer;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.govern.DependencyModule;
import io.harness.yaml.core.ExecutionElement;

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
    bind(new TypeLiteral<ProtobufSerializer<ExecutionElement>>() {}).toInstance(new ExecutionProtobufSerializer());
    bind(new TypeLiteral<ProtobufSerializer<RunStepInfo>>() {}).toInstance(new RunStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufSerializer<SaveCacheStepInfo>>() {}).toInstance(new SaveCacheStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufSerializer<RestoreCacheStepInfo>>() {})
        .toInstance(new RestoreCacheStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufSerializer<PublishStepInfo>>() {}).toInstance(new PublishStepProtobufSerializer());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of();
  }
}
