package io.harness.serializer.spring;

import io.harness.spring.AliasRegistrar;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.UseFromStage;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.extended.ci.codebase.CodeBaseType;
import io.harness.yaml.extended.ci.codebase.impl.GitHubCodeBase;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */

public class YamlBeansAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("io.harness.yaml.core.useFromStage", UseFromStage.class);
    orchestrationElements.put("io.harness.yaml.core.stepElement", StepElement.class);
    orchestrationElements.put("io.harness.yaml.core.executionElement", ExecutionElement.class);
    orchestrationElements.put("io.harness.yaml.core.stageElement", StageElement.class);
    orchestrationElements.put("io.harness.yaml.core.stepGroupElement", StepGroupElement.class);
    orchestrationElements.put("io.harness.yaml.core.parallelStepElement", ParallelStepElement.class);
    orchestrationElements.put("io.harness.yaml.core.variables.StringNGVariable", StringNGVariable.class);
    orchestrationElements.put("io.harness.yaml.core.variables.NumberNGVariable", NumberNGVariable.class);
    orchestrationElements.put("io.harness.yaml.extended.ci.CodeBase", CodeBase.class);
    orchestrationElements.put("io.harness.yaml.extended.ci.CodeBaseType", CodeBaseType.class);
    orchestrationElements.put("io.harness.yaml.extended.ci.impl.GitHubCodeBase", GitHubCodeBase.class);
  }
}
