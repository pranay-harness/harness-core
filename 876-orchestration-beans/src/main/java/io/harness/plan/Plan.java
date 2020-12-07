package io.harness.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.mappers.PlanNodeProtoMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
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
@Redesign
@FieldNameConstants(innerTypeName = "PlanKeys")
@TypeAlias("plan")
public final class Plan implements PersistentEntity {
  List<PlanNodeProto> nodes;

  @NotNull String startingNodeId;

  Map<String, String> setupAbstractions;

  Plan(List<PlanNodeProto> nodes, String startingNodeId, Map<String, String> setupAbstractions) {
    this.nodes = nodes;
    this.startingNodeId = startingNodeId;
    this.setupAbstractions = setupAbstractions;
  }

  public boolean isEmpty() {
    return EmptyPredicate.isEmpty(nodes);
  }

  public PlanNodeProto fetchStartingNode() {
    return fetchNode(startingNodeId);
  }

  public PlanNodeProto fetchNode(String nodeId) {
    int nodeIndex = Collections.binarySearch(
        nodes, PlanNodeProto.newBuilder().setUuid(nodeId).build(), Comparator.comparing(PlanNodeProto::getUuid));
    if (nodeIndex < 0) {
      throw new InvalidRequestException("No node found with Id :" + nodeId);
    }
    return nodes.get(nodeIndex);
  }

  // *********************** DO NOT OVERRIDE WITH LOMBOK BUILDER *********************

  // LOMBOK BUILDER DOES NOT PLAY WELL WHEN YOU WANT TO OVERRIDE METHODS
  // GENERATED WITH A COMBINATION OF @Singular and @Builder

  public static PlanBuilder builder() {
    return new PlanBuilder(new ArrayList<>(), null, new HashMap<>());
  }

  public static class PlanBuilder {
    private ArrayList<PlanNodeProto> nodes;
    private String startingNodeId;
    private Map<String, String> setupAbstractions;

    public PlanBuilder(ArrayList<PlanNodeProto> nodes, String startingNodeId, Map<String, String> setupAbstractions) {
      this.nodes = nodes;
      this.startingNodeId = startingNodeId;
      this.setupAbstractions = setupAbstractions;
    }

    public PlanBuilder node(PlanNode node) {
      if (this.nodes == null)
        this.nodes = new ArrayList<>();
      this.nodes.add(PlanNodeProtoMapper.toPlanNodeProto(node));
      return this;
    }

    public PlanBuilder nodes(Collection<? extends PlanNode> nodes) {
      if (this.nodes == null)
        this.nodes = new ArrayList<>();
      this.nodes.addAll(nodes.stream().map(PlanNodeProtoMapper::toPlanNodeProto).collect(Collectors.toList()));
      return this;
    }

    public PlanBuilder startingNodeId(String startingNodeId) {
      this.startingNodeId = startingNodeId;
      return this;
    }

    public PlanBuilder setupAbstractions(Map<String, String> setupAbstractions) {
      this.setupAbstractions = setupAbstractions;
      return this;
    }

    public Plan build() {
      if (EmptyPredicate.isEmpty(this.nodes)) {
        return internalBuild();
      }
      this.nodes.sort(Comparator.comparing(PlanNodeProto::getUuid));
      return internalBuild();
    }

    public Plan internalBuild() {
      List<PlanNodeProto> nodes;
      switch (this.nodes == null ? 0 : this.nodes.size()) {
        case 0:
          nodes = Collections.emptyList();
          break;
        case 1:
          nodes = Collections.singletonList(this.nodes.get(0));
          break;
        default:
          nodes = Collections.unmodifiableList(new ArrayList<>(this.nodes));
      }

      return new Plan(nodes, startingNodeId, setupAbstractions);
    }

    public String toString() {
      return "Plan.PlanBuilder(nodes=" + this.nodes + ", startingNodeId=" + this.startingNodeId
          + ", setupAbstractions=" + this.setupAbstractions + ")";
    }
  }
}
