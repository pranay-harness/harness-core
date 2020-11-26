package io.harness.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.PersistentEntity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

/**
 * This is the plan we want to execute during the execution
 * It contains a list of ExecutionNode and stating point
 * This is contained as a list sorted by the uuid to quick retrieval.
 *
 * Do not want this to be a map as we will lost the ability to query the database by node properties
 * This will be required by the apps to performs some migrations etc.
 *
 * This was a major pain point for the design of our StateMachine.
 *
 * With this approach we can crate iterators over these and perform the migrations
 */

@OwnedBy(CDC)
@Value
@Builder(buildMethodName = "internalBuild")
@Redesign
@FieldNameConstants(innerTypeName = "PlanKeys")
@TypeAlias("plan")
public class Plan implements PersistentEntity {
  @Singular List<PlanNode> nodes;

  @NotNull String startingNodeId;

  Map<String, String> setupAbstractions;

  public boolean isEmpty() {
    return EmptyPredicate.isEmpty(nodes);
  }

  public PlanNode fetchStartingNode() {
    return fetchNode(startingNodeId);
  }

  public PlanNode fetchNode(String nodeId) {
    int nodeIndex = Collections.binarySearch(
        nodes, PlanNode.builder().uuid(nodeId).build(), Comparator.comparing(PlanNode::getUuid));
    if (nodeIndex < 0) {
      throw new InvalidRequestException("No node found with Id :" + nodeId);
    }
    return nodes.get(nodeIndex);
  }

  public static class PlanBuilder {
    public Plan build() {
      if (EmptyPredicate.isEmpty(nodes)) {
        return internalBuild();
      }
      nodes.sort(Comparator.comparing(PlanNode::getUuid));
      return internalBuild();
    }
  }
}
