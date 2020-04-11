package io.harness.engine;

import com.google.inject.Inject;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.refrences.RefObject;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.state.io.StateTransput;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class EngineHelper {
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private ResolverRegistry resolverRegistry;

  List<Adviser> obtainAdvisers(List<AdviserObtainment> obtainments) {
    return obtainments.stream().map(obtainment -> adviserRegistry.obtain(obtainment)).collect(Collectors.toList());
  }

  List<Facilitator> obtainFacilitators(List<FacilitatorObtainment> obtainments) {
    return obtainments.stream().map(obtainment -> facilitatorRegistry.obtain(obtainment)).collect(Collectors.toList());
  }

  List<StateTransput> obtainInputs(List<RefObject> refObjects) {
    return refObjects.stream()
        .map(refObject -> resolverRegistry.obtain(refObject.getRefType()).resolve(refObject))
        .collect(Collectors.toList());
  }
}
