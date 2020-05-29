package graph;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.annotations.Beta;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.steps.AbstractStepWithMetaInfo;
import io.harness.yaml.core.Graph;
import io.harness.yaml.core.Parallel;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.auxiliary.intfc.StepWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Singleton
public class StepInfoGraphConverter {
  @Inject private GraphOperations<AbstractStepWithMetaInfo> operations;

  /**
   * Converts list of {@link ExecutionSection} to StepInfoGraph
   * @param sections list of section coming from yaml conversion
   * @return Graph containing steps
   */
  public StepInfoGraph convert(List<ExecutionSection> sections) {
    final StepInfoGraph graph = StepInfoGraph.builder().build();
    StepInfoGraph currentSectionGraph;
    if (isEmpty(sections)) {
      // return empty graph.
      return graph;
    }
    for (ExecutionSection section : sections) {
      if (section instanceof AbstractStepWithMetaInfo) {
        // process single step
        currentSectionGraph = handleStepSection((AbstractStepWithMetaInfo) section);
      } else if (section instanceof Parallel) {
        // process parallel section
        currentSectionGraph = handleParallelSection((Parallel) section);
      } else if (section instanceof Graph) {
        // process graph section
        currentSectionGraph = handleGraphSection((Graph) section);
      } else {
        throw new IllegalArgumentException("ExecutionSection " + section.getClass() + " is not supported");
      }

      if (!isEmpty(currentSectionGraph.getAllNodes())) {
        final Set<AbstractStepWithMetaInfo> roots = operations.findRoots(currentSectionGraph);
        final Set<AbstractStepWithMetaInfo> leaves = operations.findLeafs(graph);

        // add all nodes to StepInfoGraph from graph section
        currentSectionGraph.getAllNodes().forEach(graph::addNode);

        // connect current section roots to graph leaves
        if (!isEmpty(roots) && !isEmpty(leaves)) {
          leaves.forEach(leaf -> roots.forEach(root -> graph.addEdge(leaf, root)));
        }
        // add adjacency list of current section graph to graph
        for (AbstractStepWithMetaInfo fromNode : currentSectionGraph.getAllNodes()) {
          Set<String> edges = currentSectionGraph.getEdges(fromNode.getIdentifier());
          if (!isEmpty(edges)) {
            for (String edge : edges) {
              AbstractStepWithMetaInfo toNode = currentSectionGraph.getNode(edge);
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

  private StepInfoGraph handleStepSection(AbstractStepWithMetaInfo step) {
    StepInfoGraph stepInfoGraph = StepInfoGraph.builder().build();
    // skip if null
    if (step == null) {
      return stepInfoGraph;
    }
    // add node
    stepInfoGraph.addNode(step);
    return stepInfoGraph;
  }

  private StepInfoGraph handleParallelSection(Parallel parallel) {
    StepInfoGraph stepInfoGraph = StepInfoGraph.builder().build();

    List<StepWrapper> parallelSections = parallel.getSections();
    // skip if null
    if (isEmpty(parallelSections)) {
      return stepInfoGraph;
    }

    // add each node
    parallelSections.stream().map(step -> (AbstractStepWithMetaInfo) step).forEach(stepInfoGraph::addNode);

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

    graph.getSections().stream().map(step -> (AbstractStepWithMetaInfo) step).forEach(stepInfoGraph::addNode);

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
