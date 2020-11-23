package io.harness;

import io.harness.beans.serializer.ExecutionProtobufSerializer;
import io.harness.beans.serializer.PluginStepProtobufSerializer;
import io.harness.beans.serializer.ProtobufSerializer;
import io.harness.beans.serializer.PublishStepProtobufSerializer;
import io.harness.beans.serializer.RestoreCacheStepProtobufSerializer;
import io.harness.beans.serializer.RunStepProtobufSerializer;
import io.harness.beans.serializer.SaveCacheStepProtobufSerializer;
import io.harness.beans.serializer.TestIntelligenceStepProtobufSerializer;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.beans.steps.stepinfo.TestIntelligenceStepInfo;
import io.harness.yaml.core.ExecutionElement;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import graph.GraphOperations;

public class CIBeansModule extends AbstractModule {
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
    bind(new TypeLiteral<ProtobufSerializer<PluginStepInfo>>() {}).toInstance(new PluginStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufSerializer<SaveCacheStepInfo>>() {}).toInstance(new SaveCacheStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufSerializer<TestIntelligenceStepInfo>>() {
    }).toInstance(new TestIntelligenceStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufSerializer<RestoreCacheStepInfo>>() {
    }).toInstance(new RestoreCacheStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufSerializer<PublishStepInfo>>() {}).toInstance(new PublishStepProtobufSerializer());
  }
}
