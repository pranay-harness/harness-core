package io.harness;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.serializer.ExecutionProtobufSerializer;
import io.harness.beans.serializer.PluginCompatibleStepSerializer;
import io.harness.beans.serializer.PluginStepProtobufSerializer;
import io.harness.beans.serializer.ProtobufSerializer;
import io.harness.beans.serializer.ProtobufStepSerializer;
import io.harness.beans.serializer.RunStepProtobufSerializer;
import io.harness.beans.serializer.RunTestsStepProtobufSerializer;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.plancreator.execution.ExecutionElementConfig;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

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
    bind(new TypeLiteral<ProtobufSerializer<ExecutionElementConfig>>() {
    }).toInstance(new ExecutionProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<RunStepInfo>>() {}).toInstance(new RunStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<PluginStepInfo>>() {}).toInstance(new PluginStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<RunTestsStepInfo>>() {
    }).toInstance(new RunTestsStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<PluginCompatibleStep>>() {
    }).toInstance(new PluginCompatibleStepSerializer());
  }
}
