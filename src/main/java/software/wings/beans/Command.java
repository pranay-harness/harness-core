package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Graph.Node;
import software.wings.utils.MapperUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.validation.constraints.NotNull;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
public class Command extends CommandUnit {
  private String referenceId;
  @NotNull private Graph graph;

  @NotEmpty private List<CommandUnit> commandUnits = Lists.newArrayList();

  /**
   * Instantiates a new command.
   */
  public Command() {
    super(CommandUnitType.COMMAND);
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public Graph getGraph() {
    return graph;
  }

  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  public List<CommandUnit> getCommandUnits() {
    return commandUnits;
  }

  public void setCommandUnits(List<CommandUnit> commandUnits) {
    this.commandUnits = commandUnits;
  }

  /**
   * Transform graph.
   */
  public void transformGraph() {
    setName(graph.getGraphName());
    Iterator<Node> pipelineIterator = graph.getLinearGraphIterator();
    while (pipelineIterator.hasNext()) {
      Node node = pipelineIterator.next();

      if (node.isOrigin()) {
        continue;
      }

      CommandUnitType type = CommandUnitType.valueOf(node.getType().toUpperCase());

      CommandUnit commandUnit = type.newInstance();
      MapperUtils.mapObject(node.getProperties(), commandUnit);
      commandUnits.add(commandUnit);
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Command command = (Command) obj;
    return Objects.equal(referenceId, command.referenceId) && Objects.equal(commandUnits, command.commandUnits);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(referenceId, commandUnits);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("referenceId", referenceId)
        .add("graph", graph)
        .add("commandUnits", commandUnits)
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String referenceId;
    private Graph graph;
    private List<CommandUnit> commandUnits = Lists.newArrayList();
    private String name;
    private String serviceId;
    private ExecutionResult executionResult;

    private Builder() {}

    /**
     * A command.
     *
     * @return the builder
     */
    public static Builder aCommand() {
      return new Builder();
    }

    /**
     * With reference id.
     *
     * @param referenceId the reference id
     * @return the builder
     */
    public Builder withReferenceId(String referenceId) {
      this.referenceId = referenceId;
      return this;
    }

    /**
     * With graph.
     *
     * @param graph the graph
     * @return the builder
     */
    public Builder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    /**
     * Adds the command units.
     *
     * @param commandUnits the command units
     * @return the builder
     */
    public Builder addCommandUnits(CommandUnit... commandUnits) {
      this.commandUnits.addAll(Arrays.asList(commandUnits));
      return this;
    }

    /**
     * With command units.
     *
     * @param commandUnits the command units
     * @return the builder
     */
    public Builder withCommandUnits(List<CommandUnit> commandUnits) {
      this.commandUnits = commandUnits;
      return this;
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With service id.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With execution result.
     *
     * @param executionResult the execution result
     * @return the builder
     */
    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aCommand()
          .withReferenceId(referenceId)
          .withGraph(graph)
          .withCommandUnits(commandUnits)
          .withName(name)
          .withServiceId(serviceId)
          .withExecutionResult(executionResult);
    }

    /**
     * Builds the.
     *
     * @return the command
     */
    public Command build() {
      Command command = new Command();
      command.setReferenceId(referenceId);
      command.setGraph(graph);
      command.setCommandUnits(commandUnits);
      command.setName(name);
      command.setServiceId(serviceId);
      command.setExecutionResult(executionResult);
      return command;
    }
  }
}
