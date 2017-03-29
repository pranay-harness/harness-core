package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;
import java.util.Set;

/**
 * Created by rishi on 3/28/17.
 */
@JsonTypeName("CANARY")
public class CustomOrchestrationWorkflow extends OrchestrationWorkflow {
  public CustomOrchestrationWorkflow() {
    setWorkflowOrchestrationType(OrchestrationWorkflowType.CUSTOM);
  }

  private Graph graph;

  /**
   * Gets graph.
   *
   * @return the graph
   */
  public Graph getGraph() {
    return graph;
  }

  /**
   * Sets graph.
   *
   * @param graph the graph
   */
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  @Override
  public List<String> getServiceIds() {
    return null;
  }

  @Override
  public void onSave() {}

  @Override
  public void onLoad() {}

  @Override
  public Set<EntityType> getRequiredEntityTypes() {
    return null;
  }

  @Override
  public void setRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {}

  public static final class CustomOrchestrationWorkflowBuilder {
    private Graph graph;

    private CustomOrchestrationWorkflowBuilder() {}

    public static CustomOrchestrationWorkflowBuilder aCustomOrchestrationWorkflow() {
      return new CustomOrchestrationWorkflowBuilder();
    }

    public CustomOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public CustomOrchestrationWorkflow build() {
      CustomOrchestrationWorkflow customOrchestrationWorkflow = new CustomOrchestrationWorkflow();
      customOrchestrationWorkflow.setGraph(graph);
      return customOrchestrationWorkflow;
    }
  }
}
