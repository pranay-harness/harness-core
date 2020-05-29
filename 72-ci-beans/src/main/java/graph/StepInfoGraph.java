package graph;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.steps.AbstractStepWithMetaInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
public class StepInfoGraph implements Graph<AbstractStepWithMetaInfo> {
  @Builder.Default Map<String, Set<String>> adjacencyList = new HashMap<>();
  @Builder.Default Map<String, AbstractStepWithMetaInfo> nodesMap = new HashMap<>();

  @Getter @Setter private List<AbstractStepWithMetaInfo> steps;

  public static final String NIL_NODE = "00000000-0000-0000-0000-000000000000";

  public String getStartNodeUuid() {
    if (isEmpty(steps)) {
      throw new IllegalStateException("Steps list can not be empty");
    }
    return steps.get(0).getStepMetadata().getUuid();
  }

  public static boolean isNILStepUuId(String uuId) {
    return uuId.equals(NIL_NODE);
  }

  public List<String> getNextNodeUuids(AbstractStepWithMetaInfo currentStep) {
    final Set<String> childNodes = adjacencyList.get(currentStep.getIdentifier());
    List<String> result = childNodes.stream()
                              .map(nodesMap::get)
                              .map(ciStep -> ciStep.getStepMetadata().getUuid())
                              .collect(Collectors.toList());

    return result.isEmpty() ? Collections.singletonList(NIL_NODE) : result;
  }

  public void addNode(AbstractStepWithMetaInfo step) {
    nodesMap.putIfAbsent(step.getIdentifier(), step);
    adjacencyList.putIfAbsent(step.getIdentifier(), new HashSet<>());
  }

  public void addEdge(AbstractStepWithMetaInfo from, AbstractStepWithMetaInfo to) {
    // Make sure to add steps as well
    addNode(from);
    addNode(to);
    // add dependency
    adjacencyList.get(from.getIdentifier()).add(to.getIdentifier());
  }

  @Override
  public Set<String> getEdges(String nodeId) {
    return adjacencyList.get(nodeId);
  }

  @Override
  public AbstractStepWithMetaInfo getNode(String nodeId) {
    return nodesMap.get(nodeId);
  }

  @Override
  public List<AbstractStepWithMetaInfo> getAllNodes() {
    return new ArrayList<>(nodesMap.values());
  }
}
