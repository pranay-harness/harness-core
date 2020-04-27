package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.ExecutionPlan;
import io.harness.references.RefObject;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StateRegistry;
import io.harness.state.State;
import io.harness.state.StateType;
import io.harness.state.execution.ExecutionInstance;
import io.harness.state.execution.ExecutionInstance.ExecutionInstanceKeys;
import io.harness.state.io.StateTransput;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Redesign
public class EngineObtainmentHelper {
  @Inject private HPersistence hPersistence;
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

  public List<StateTransput> obtainInputs(
      Ambiance ambiance, List<RefObject> refObjects, List<StateTransput> additionalInputs) {
    if (isEmpty(refObjects)) {
      return Collections.emptyList();
    }
    List<StateTransput> inputs =
        refObjects.stream()
            .map(refObject -> resolverRegistry.obtain(refObject.getRefType()).resolve(ambiance, refObject))
            .collect(Collectors.toList());
    inputs.addAll(additionalInputs);
    return inputs;
  }

  public State obtainState(@NonNull StateType stateType) {
    return stateRegistry.obtain(stateType);
  }

  public ExecutionNode fetchExecutionNode(String nodeId, String executionInstanceId) {
    ExecutionInstance instance =
        hPersistence.createQuery(ExecutionInstance.class).filter(ExecutionInstanceKeys.uuid, executionInstanceId).get();
    if (instance == null) {
      throw new InvalidRequestException("Execution Instance is null for id : " + executionInstanceId);
    }
    ExecutionPlan plan = instance.getExecutionPlan();
    return plan.fetchNode(nodeId);
  }
}
