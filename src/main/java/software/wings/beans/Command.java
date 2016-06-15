package software.wings.beans;

import static software.wings.beans.CatalogNames.SERVICE_COMMAND;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Graph.Node;
import software.wings.sm.EnumData;
import software.wings.utils.MapperUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
@Attributes(title = "COMMAND")
public class Command extends CommandUnit {
  @EnumData(catalog = SERVICE_COMMAND) @Attributes(title = "Name") private String referenceId;
  @SchemaIgnore @NotNull private Graph graph;

  @SchemaIgnore @NotEmpty private List<CommandUnit> commandUnits = Lists.newArrayList();

  /**
   * Instantiates a new command.
   */
  public Command() {
    super(CommandUnitType.COMMAND);
  }

  /**
   * Gets reference id.
   *
   * @return the reference id
   */
  public String getReferenceId() {
    return referenceId;
  }

  /**
   * Sets reference id.
   *
   * @param referenceId the reference id
   */
  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  /**
   * Gets graph.
   *
   * @return the graph
   */
  @SchemaIgnore
  public Graph getGraph() {
    return graph;
  }

  /**
   * Sets graph.
   *
   * @param graph the graph
   */
  @SchemaIgnore
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  /**
   * Gets command units.
   *
   * @return the command units
   */
  @SchemaIgnore
  public List<CommandUnit> getCommandUnits() {
    return commandUnits;
  }

  /**
   * Sets command units.
   *
   * @param commandUnits the command units
   */
  @SchemaIgnore
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
      commandUnit.setName(node.getName());
      commandUnits.add(commandUnit);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(referenceId, graph, commandUnits);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Command other = (Command) obj;
    return Objects.equals(this.referenceId, other.referenceId) && Objects.equals(this.graph, other.graph)
        && Objects.equals(this.commandUnits, other.commandUnits);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public boolean isArtifactNeeded() {
    return commandUnits.stream().filter(CommandUnit::isArtifactNeeded).findFirst().isPresent();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("referenceId", referenceId)
        .add("graph", graph)
        .add("commandUnits", commandUnits)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String referenceId;
    private Graph graph;
    private List<CommandUnit> commandUnits = Lists.newArrayList();
    private String name;
    private ExecutionResult executionResult;
    private boolean artifactNeeded;

    private Builder() {}

    /**
     * A command builder.
     *
     * @return the builder
     */
    public static Builder aCommand() {
      return new Builder();
    }

    /**
     * With reference id builder.
     *
     * @param referenceId the reference id
     * @return the builder
     */
    public Builder withReferenceId(String referenceId) {
      this.referenceId = referenceId;
      return this;
    }

    /**
     * With graph builder.
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
     * With command units builder.
     *
     * @param commandUnits the command units
     * @return the builder
     */
    public Builder withCommandUnits(List<CommandUnit> commandUnits) {
      this.commandUnits = commandUnits;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With execution result builder.
     *
     * @param executionResult the execution result
     * @return the builder
     */
    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    /**
     * With artifact needed builder.
     *
     * @param artifactNeeded the artifact needed
     * @return the builder
     */
    public Builder withArtifactNeeded(boolean artifactNeeded) {
      this.artifactNeeded = artifactNeeded;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCommand()
          .withReferenceId(referenceId)
          .withGraph(graph)
          .withCommandUnits(commandUnits)
          .withName(name)
          .withExecutionResult(executionResult)
          .withArtifactNeeded(artifactNeeded);
    }

    /**
     * Build command.
     *
     * @return the command
     */
    public Command build() {
      Command command = new Command();
      command.setReferenceId(referenceId);
      command.setGraph(graph);
      command.setCommandUnits(commandUnits);
      command.setName(name);
      command.setExecutionResult(executionResult);
      command.setArtifactNeeded(artifactNeeded);
      return command;
    }
  }
}
