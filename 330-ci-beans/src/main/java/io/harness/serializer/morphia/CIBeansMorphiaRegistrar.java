/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import io.harness.beans.outcomes.LiteEnginePodDetailsOutcome;
import io.harness.beans.sweepingoutputs.ContainerPortDetails;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.sweepingoutputs.PodCleanupDetails;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.ci.stdvars.BuildStandardVariables;
import io.harness.ci.stdvars.GitVariables;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CIBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(BuildNumberDetails.class);
    set.add(CIBuild.class);
    set.add(K8PodDetails.class);
    set.add(StepTaskDetails.class);
    set.add(BuildStandardVariables.class);
    set.add(GitVariables.class);
    set.add(ContextElement.class);
    set.add(PodCleanupDetails.class);
    set.add(ContainerPortDetails.class);
    set.add(LiteEnginePodDetailsOutcome.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("sweepingoutputs.K8PodDetails", K8PodDetails.class);
    w.put("sweepingoutputs.PodCleanupDetails", PodCleanupDetails.class);
  }
}
