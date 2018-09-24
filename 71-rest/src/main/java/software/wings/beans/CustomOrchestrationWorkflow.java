package software.wings.beans;

import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by rishi on 3/28/17.
 */
@JsonTypeName("CUSTOM")
public class CustomOrchestrationWorkflow extends OrchestrationWorkflow {
  public CustomOrchestrationWorkflow() {
    setOrchestrationWorkflowType(OrchestrationWorkflowType.CUSTOM);
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

  @Override
  public boolean validate() {
    return true;
  }

  @Override
  public OrchestrationWorkflow cloneInternal() {
    return aCustomOrchestrationWorkflow().withGraph(getGraph()).build();
  }

  @Override
  public List<Variable> getUserVariables() {
    return null;
  }

  @Override
  public void setCloneMetadata(Map<String, String> serviceIdMapping) {}

  @Override
  public List<String> getInfraMappingIds() {
    return new ArrayList<>();
  }

  @Override
  public boolean needCloudProvider() {
    return true;
  }

  @Override
  public List<NotificationRule> getNotificationRules() {
    return Collections.emptyList();
  }

  @Override
  public void setNotificationRules(List<NotificationRule> notificationRules) {}

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CustomOrchestrationWorkflow that = (CustomOrchestrationWorkflow) o;

    return graph != null ? graph.equals(that.graph) : that.graph == null;
  }

  @Override
  public int hashCode() {
    return graph != null ? graph.hashCode() : 0;
  }

  public static final class CustomOrchestrationWorkflowBuilder {
    private Graph graph;
    private boolean valid;

    private CustomOrchestrationWorkflowBuilder() {}

    public static CustomOrchestrationWorkflowBuilder aCustomOrchestrationWorkflow() {
      return new CustomOrchestrationWorkflowBuilder();
    }

    public CustomOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public CustomOrchestrationWorkflowBuilder withValid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public CustomOrchestrationWorkflowBuilder but() {
      return aCustomOrchestrationWorkflow().withGraph(graph).withValid(valid);
    }

    public CustomOrchestrationWorkflow build() {
      CustomOrchestrationWorkflow customOrchestrationWorkflow = new CustomOrchestrationWorkflow();
      customOrchestrationWorkflow.setGraph(graph);
      customOrchestrationWorkflow.setValid(valid);
      return customOrchestrationWorkflow;
    }
  }
}
