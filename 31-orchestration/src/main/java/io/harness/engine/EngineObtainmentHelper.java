package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.references.RefObject;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.resolvers.Resolver;
import io.harness.state.io.StateTransput;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Redesign
@ExcludeRedesign
public class EngineObtainmentHelper {
  @Inject private HPersistence hPersistence;
  @Inject private ResolverRegistry resolverRegistry;

  public List<StateTransput> obtainInputs(
      Ambiance ambiance, List<RefObject> refObjects, List<? extends StateTransput> additionalInputs) {
    List<StateTransput> inputs = new ArrayList<>();

    if (additionalInputs != null) {
      inputs.addAll(additionalInputs);
    }
    if (!isEmpty(refObjects)) {
      for (RefObject refObject : refObjects) {
        Resolver resolver = resolverRegistry.obtain(refObject.getRefType());
        inputs.add(resolver.resolve(ambiance, refObject));
      }
    }
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
