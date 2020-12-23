package io.harness;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.serializer.ExecutionProtobufSerializer;
import io.harness.beans.serializer.PluginCompatibleStepSerializer;
import io.harness.beans.serializer.PluginStepProtobufSerializer;
import io.harness.beans.serializer.ProtobufSerializer;
import io.harness.beans.serializer.ProtobufStepSerializer;
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
import io.harness.plancreator.execution.ExecutionElementConfig;

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
    bind(new TypeLiteral<ProtobufSerializer<ExecutionElementConfig>>() {
    }).toInstance(new ExecutionProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<RunStepInfo>>() {}).toInstance(new RunStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<PluginStepInfo>>() {}).toInstance(new PluginStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<SaveCacheStepInfo>>() {
    }).toInstance(new SaveCacheStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<RestoreCacheStepInfo>>() {
    }).toInstance(new RestoreCacheStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<TestIntelligenceStepInfo>>() {
    }).toInstance(new TestIntelligenceStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<PublishStepInfo>>() {}).toInstance(new PublishStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<PluginCompatibleStep>>() {
    }).toInstance(new PluginCompatibleStepSerializer());
  }
}
