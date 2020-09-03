package io.harness.serializer.morphia;

import io.harness.beans.CIPipeline;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CIBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CIPipeline.class);
    set.add(BuildNumber.class);
    set.add(CIBuild.class);
    set.add(K8PodDetails.class);
    set.add(ContextElement.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("sweepingoutputs.K8PodDetails", K8PodDetails.class);
  }
}
