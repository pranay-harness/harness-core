package io.harness.serializer.spring;

import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */

public class DelegateTaskBeansAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("pcfManifestsPackage", PcfManifestsPackage.class);
    orchestrationElements.put("stepMapOutput", StepMapOutput.class);
  }
}
