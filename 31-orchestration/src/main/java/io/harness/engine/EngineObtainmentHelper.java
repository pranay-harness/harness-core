package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.refrences.RefObject;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StateRegistry;
import io.harness.state.State;
import io.harness.state.io.StateTransput;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EngineObtainmentHelper {
  @Inject private StateRegistry stateRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private ResolverRegistry resolverRegistry;

  public List<Adviser> obtainAdvisers(List<AdviserObtainment> obtainments) {
    if (isEmpty(obtainments)) {
      return Collections.emptyList();
    }
    return obtainments.stream().map(obtainment -> adviserRegistry.obtain(obtainment)).collect(Collectors.toList());
  }

  public List<Facilitator> obtainFacilitators(List<FacilitatorObtainment> obtainments) {
    if (isEmpty(obtainments)) {
      return Collections.emptyList();
    }
    return obtainments.stream().map(obtainment -> facilitatorRegistry.obtain(obtainment)).collect(Collectors.toList());
  }

  public List<StateTransput> obtainInputs(List<RefObject> refObjects) {
    if (isEmpty(refObjects)) {
      return Collections.emptyList();
    }
    return refObjects.stream()
        .map(refObject -> resolverRegistry.obtain(refObject.getRefType()).resolve(refObject))
        .collect(Collectors.toList());
  }

  public State obtainState(String stateType) {
    if (isEmpty(stateType)) {
      return null;
    }
    return stateRegistry.obtain(stateType);
  }
}
