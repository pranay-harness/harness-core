package software.wings.beans.command;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Node;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;
import software.wings.utils.ArtifactType;
import software.wings.utils.ContainerFamily;
import software.wings.utils.MapperUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
@JsonTypeName("COMMAND")
@Attributes(title = "Command")
@Entity(value = "commands")
public class Command extends Base implements CommandUnit {
  @SchemaIgnore private String name;
  @SchemaIgnore private CommandUnitType commandUnitType;
  @SchemaIgnore private ExecutionResult executionResult;

  @SchemaIgnore private boolean artifactNeeded = false;

  @SchemaIgnore private String originEntityId;

  @SchemaIgnore private ContainerFamily containerFamily;

  @SchemaIgnore private ArtifactType artifactType;

  @Expand(dataProvider = ServiceResourceServiceImpl.class)
  @EnumData(enumDataProvider = ServiceResourceServiceImpl.class)
  @Attributes(title = "Name")
  private String referenceId;

  @SchemaIgnore @NotNull private Graph graph;

  @SchemaIgnore private Long version;

  @SchemaIgnore @NotEmpty private List<CommandUnit> commandUnits = Lists.newArrayList();

  private CommandType commandType = CommandType.OTHER;

  public Command() {
    this.commandUnitType = CommandUnitType.COMMAND;
  }

  /**
   * Instantiates a new command.
   */
  @Override
  public ExecutionResult execute(CommandExecutionContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CommandUnitType getCommandUnitType() {
    return commandUnitType;
  }

  @Override
  public void setCommandUnitType(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  @Override
  public ExecutionResult getExecutionResult() {
    return executionResult;
  }

  @Override
  public void setExecutionResult(ExecutionResult executionResult) {
    this.executionResult = executionResult;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
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
   * Getter for property 'version'.
   *
   * @return Value for property 'version'.
   */
  @SchemaIgnore
  public Long getVersion() {
    return Optional.ofNullable(version).orElse(1L);
  }

  /**
   * Setter for property 'version'.
   *
   * @param version Value to set for property 'version'.
   */
  @SchemaIgnore
  public void setVersion(Long version) {
    this.version = version;
  }

  /**
   * Getter for property 'originEntityId'.
   *
   * @return Value for property 'originEntityId'.
   */
  public String getOriginEntityId() {
    return originEntityId;
  }

  /**
   * Setter for property 'originEntityId'.
   *
   * @param originEntityId Value to set for property 'originEntityId'.
   */
  public void setOriginEntityId(String originEntityId) {
    this.originEntityId = originEntityId;
  }

  /**
   * Getter for property 'containerFamily'.
   *
   * @return Value for property 'containerFamily'.
   */
  public ContainerFamily getContainerFamily() {
    return containerFamily;
  }

  /**
   * Setter for property 'containerFamily'.
   *
   * @param containerFamily Value to set for property 'containerFamily'.
   */
  public void setContainerFamily(ContainerFamily containerFamily) {
    this.containerFamily = containerFamily;
  }

  /**
   * Getter for property 'artifactType'.
   *
   * @return Value for property 'artifactType'.
   */
  public ArtifactType getArtifactType() {
    return artifactType;
  }

  /**
   * Setter for property 'artifactType'.
   *
   * @param artifactType Value to set for property 'artifactType'.
   */
  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  public CommandType getCommandType() {
    return commandType;
  }

  public void setCommandType(CommandType commandType) {
    this.commandType = commandType;
  }

  /**
   * Transform graph.
   */
  public void transformGraph() {
    setName(graph.getGraphName());
    Iterator<Node> pipelineIterator = graph.getLinearGraphIterator();
    while (pipelineIterator.hasNext()) {
      Node node = pipelineIterator.next();
      CommandUnitType type = CommandUnitType.valueOf(node.getType().toUpperCase());

      CommandUnit commandUnit = type.newInstance("");
      MapperUtils.mapObject(node.getProperties(), type.getTypeClass().cast(commandUnit));
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
  @SchemaIgnore
  @Override
  public boolean isArtifactNeeded() {
    return commandUnits.stream().filter(CommandUnit::isArtifactNeeded).findFirst().isPresent();
  }

  @Override
  public String deploymentType() {
    return commandUnits.get(0).deploymentType();
  }

  @Override
  public void setArtifactNeeded(boolean artifactNeeded) {}

  @SchemaIgnore
  @Override
  public String getAppId() {
    return super.getAppId();
  }

  @SchemaIgnore
  @Override
  public EmbeddedUser getCreatedBy() {
    return super.getCreatedBy();
  }

  @SchemaIgnore
  @Override
  public EmbeddedUser getLastUpdatedBy() {
    return super.getLastUpdatedBy();
  }

  @SchemaIgnore
  @Override
  public long getCreatedAt() {
    return super.getCreatedAt();
  }

  @SchemaIgnore
  @Override
  public long getLastUpdatedAt() {
    return super.getLastUpdatedAt();
  }

  @SchemaIgnore
  @Override
  public String getUuid() {
    return super.getUuid();
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
    private CommandType commandType = CommandType.OTHER;

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
     * With command type
     *
     * @param commandType the command type
     * @return the builder
     */
    public Builder withCommandType(CommandType commandType) {
      this.commandType = commandType;
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
          .withArtifactNeeded(artifactNeeded)
          .withCommandType(commandType);
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
      command.setCommandType(commandType);
      return command;
    }
  }
}
