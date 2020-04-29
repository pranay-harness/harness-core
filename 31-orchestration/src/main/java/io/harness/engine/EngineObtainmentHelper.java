package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.references.RefObject;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.state.execution.PlanExecution;
import io.harness.state.execution.PlanExecution.PlanExecutionKeys;
import io.harness.state.io.StateTransput;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Redesign
public class EngineObtainmentHelper {
  @Inject private HPersistence hPersistence;
  @Inject private ResolverRegistry resolverRegistry;

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

  public ExecutionNode fetchExecutionNode(String nodeId, String executionInstanceId) {
    PlanExecution instance =
        hPersistence.createQuery(PlanExecution.class).filter(PlanExecutionKeys.uuid, executionInstanceId).get();
    if (instance == null) {
      throw new InvalidRequestException("Execution Instance is null for id : " + executionInstanceId);
    }
    Plan plan = instance.getPlan();
    return plan.fetchNode(nodeId);
  }
}
