package graph;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.steps.CIStepInfo;
import io.harness.yaml.core.Graph;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.StepSpecType;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.common.annotations.Beta;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class StepInfoGraphConverter {
  @Inject private GraphOperations<CIStepInfo> operations;

  /**
   * Converts list of {@link ExecutionWrapper} to StepInfoGraph
   * @param sections list of section coming from yaml conversion
   * @return Graph containing steps
   */
  public StepInfoGraph convert(List<ExecutionWrapper> sections) {
    final StepInfoGraph graph = StepInfoGraph.builder().build();
    StepInfoGraph currentSectionGraph;
    if (isEmpty(sections)) {
      // return empty graph.
      return graph;
    }
    for (ExecutionWrapper section : sections) {
      if (section instanceof StepElement) {
        // process single step
        StepSpecType spec = ((StepElement) section).getStepSpecType();
        currentSectionGraph = handleStepSection((CIStepInfo) spec);
      } else if (section instanceof ParallelStepElement) {
        // process parallel section
        currentSectionGraph = handleParallelSection((ParallelStepElement) section);
      } else if (section instanceof Graph) {
        // process graph section
        currentSectionGraph = handleGraphSection((Graph) section);
      } else {
        throw new IllegalArgumentException("ExecutionSection " + section.getClass() + " is not supported");
      }

      if (!isEmpty(currentSectionGraph.getAllNodes())) {
        final Set<CIStepInfo> roots = operations.findRoots(currentSectionGraph);
        final Set<CIStepInfo> leaves = operations.findLeafs(graph);

        // add all nodes to StepInfoGraph from graph section
        currentSectionGraph.getAllNodes().forEach(graph::addNode);

        // connect current section roots to graph leaves
        if (!isEmpty(roots) && !isEmpty(leaves)) {
          leaves.forEach(leaf -> roots.forEach(root -> graph.addEdge(leaf, root)));
        }
        // add adjacency list of current section graph to graph
        for (CIStepInfo fromNode : currentSectionGraph.getAllNodes()) {
          Set<String> edges = currentSectionGraph.getEdges(fromNode.getIdentifier());
          if (!isEmpty(edges)) {
            for (String edge : edges) {
              CIStepInfo toNode = currentSectionGraph.getNode(edge);
              graph.addEdge(fromNode, toNode);
            }
          }
        }
      }
    }
    // Save sequential list into ciSteps list for sequential order
    graph.setSteps(operations.topologicalSort(graph));

    return graph;
  }

  private StepInfoGraph handleStepSection(CIStepInfo step) {
    StepInfoGraph stepInfoGraph = StepInfoGraph.builder().build();
    // skip if null
    if (step == null) {
      return stepInfoGraph;
    }
    // add node
    stepInfoGraph.addNode(step);
    return stepInfoGraph;
  }

  private StepInfoGraph handleParallelSection(ParallelStepElement parallelStepElement) {
    StepInfoGraph stepInfoGraph = StepInfoGraph.builder().build();

    // TODO(harsh): Please handle StepGroup here.
    List<StepElement> parallelSections =
        parallelStepElement.getSections().stream().map(section -> (StepElement) section).collect(Collectors.toList());
    // skip if null
    if (isEmpty(parallelSections)) {
      return stepInfoGraph;
    }

    // add each node
    parallelSections.stream().map(step -> (CIStepInfo) step.getStepSpecType()).forEach(stepInfoGraph::addNode);

    // return all parallel steps as previous steps
    return stepInfoGraph;
  }

  @Beta
  private StepInfoGraph handleGraphSection(Graph graphSection) {
    StepInfoGraph stepInfoGraph = StepInfoGraph.builder().build();
    // skip if empty
    if (isEmpty(graphSection.getSections())) {
      return stepInfoGraph;
    }

    // convert yaml graph section to StepInfoGraph
    return toStepGraph(graphSection);
  }

  // converts yaml Graph section to StepInfoGraph
  private StepInfoGraph toStepGraph(Graph graph) {
    StepInfoGraph stepInfoGraph = StepInfoGraph.builder().build();
    // skip if empty
    if (isEmpty(graph.getSections())) {
      return stepInfoGraph;
    }

    graph.getSections().stream().map(step -> (CIStepInfo) step.getStepSpecType()).forEach(stepInfoGraph::addNode);

    // add dependencies
    stepInfoGraph.getAllNodes().forEach(step
        -> Optional.ofNullable(step.getDependencies())
               .map(Collection::stream)
               .orElseGet(Stream::empty)
               .map(stepInfoGraph::getNode)
               .forEach(stepDep -> stepInfoGraph.addEdge(stepDep, step))

    );
    return stepInfoGraph;
  }
}
