/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.utils.DummyOrchestrationOutcome;
import io.harness.utils.steps.TestStepParameters;

import java.util.Set;

public class OrchestrationTestMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {}

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("utils.DummyOrchestrationOutcome", DummyOrchestrationOutcome.class);
    h.put("utils.steps.TestStepParameters", TestStepParameters.class);
  }
}
