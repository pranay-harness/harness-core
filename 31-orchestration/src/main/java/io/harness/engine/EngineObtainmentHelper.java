package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.persistence.HPersistence;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.references.RefObject;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.resolvers.Resolver;
import io.harness.state.io.StepTransput;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Redesign
public class EngineObtainmentHelper {
  @Inject private HPersistence hPersistence;
  @Inject private ResolverRegistry resolverRegistry;
  @Inject private NodeExecutionService nodeExecutionService;

  public List<StepTransput> obtainInputs(
      Ambiance ambiance, List<RefObject> refObjects, List<? extends StepTransput> additionalInputs) {
    List<StepTransput> inputs = new ArrayList<>();

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

  public PlanNode fetchExecutionNode(String nodeId, String executionInstanceId) {
    PlanExecution instance =
        hPersistence.createQuery(PlanExecution.class).filter(PlanExecutionKeys.uuid, executionInstanceId).get();
    if (instance == null) {
      throw new InvalidRequestException("Execution Instance is null for id : " + executionInstanceId);
    }
    Plan plan = instance.getPlan();
    return plan.fetchNode(nodeId);
  }

  public List<StepTransput> fetchAdditionalInputs(NodeExecution nodeExecution, PlanNode node) {
    List<StepTransput> additionalInputs = new ArrayList<>();
    if (nodeExecution.getParentId() != null) {
      NodeExecution parent = nodeExecutionService.get(nodeExecution.getParentId());
      if (parent.getMode() == ExecutionMode.CHILD) {
        ChildExecutableResponse parentResponse = (ChildExecutableResponse) parent.obtainLatestExecutableResponse();
        if (parentResponse != null && parentResponse.getChildNodeId().equals(node.getUuid())
            && parentResponse.getAdditionalInputs() != null) {
          additionalInputs.addAll(parentResponse.getAdditionalInputs());
        }
      } else if (parent.getMode() == ExecutionMode.CHILDREN) {
        ChildrenExecutableResponse parentResponse =
            (ChildrenExecutableResponse) parent.obtainLatestExecutableResponse();
        if (parentResponse != null) {
          ChildrenExecutableResponse.Child child = parentResponse.getChildren()
                                                       .stream()
                                                       .filter(child1 -> child1.getChildNodeId().equals(node.getUuid()))
                                                       .findFirst()
                                                       .orElse(null);
          if (child != null && child.getAdditionalInputs() != null) {
            additionalInputs.addAll(child.getAdditionalInputs());
          }
        }
      }
    }
    return additionalInputs;
  }
}
