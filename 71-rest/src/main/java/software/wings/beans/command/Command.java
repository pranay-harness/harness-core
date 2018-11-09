package software.wings.beans.command;

import static java.util.Arrays.asList;
import static software.wings.beans.command.Command.Builder.aCommand;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.Variable;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;
import software.wings.utils.ArtifactType;
import software.wings.utils.ContainerFamily;
import software.wings.utils.MapperUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
@JsonTypeName("COMMAND")
@Attributes(title = "Command")
@Entity(value = "commands")
@Indexes(@Index(options = @IndexOptions(name = "yaml", unique = true),
    fields = { @Field("appId")
               , @Field("originEntityId"), @Field("version") }))
public class Command extends Base implements CommandUnit {
  @NotEmpty @SchemaIgnore private String name;
  @SchemaIgnore private CommandUnitType commandUnitType;
  @SchemaIgnore private CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.QUEUED;

  @SchemaIgnore private boolean artifactNeeded;
  @Deprecated @SchemaIgnore private String deploymentType;

  @NotEmpty @SchemaIgnore private String originEntityId;

  @SchemaIgnore private ContainerFamily containerFamily;

  @SchemaIgnore private ArtifactType artifactType;

  @Expand(dataProvider = ServiceResourceServiceImpl.class)
  @EnumData(enumDataProvider = ServiceResourceServiceImpl.class)
  @Attributes(title = "Name")
  private String referenceId;
  private String referenceUuid;
  @SchemaIgnore private transient Graph graph;

  @SchemaIgnore private Long version;

  @SchemaIgnore private List<CommandUnit> commandUnits = Lists.newArrayList();

  @SchemaIgnore private CommandType commandType = CommandType.OTHER;

  @SchemaIgnore private List<Variable> templateVariables = new ArrayList<>();

  public Command() {
    this.commandUnitType = CommandUnitType.COMMAND;
  }

  /**
   * Instantiates a new command.
   */
  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
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

  public CommandExecutionStatus getCommandExecutionStatus() {
    return commandExecutionStatus;
  }

  public void setCommandExecutionStatus(CommandExecutionStatus commandExecutionStatus) {
    this.commandExecutionStatus = commandExecutionStatus;
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

  @SchemaIgnore
  public CommandType getCommandType() {
    return commandType;
  }

  public void setCommandType(CommandType commandType) {
    this.commandType = commandType;
  }

  @SchemaIgnore
  public String getReferenceUuid() {
    return referenceUuid;
  }

  public void setReferenceUuid(String referenceUuid) {
    this.referenceUuid = referenceUuid;
  }

  @SchemaIgnore
  public List<Variable> getTemplateVariables() {
    return templateVariables;
  }

  public void setTemplateVariables(List<Variable> templateVariables) {
    this.templateVariables = templateVariables;
  }

  /**
   * Transform graph.
   */
  public void transformGraph() {
    setName(graph.getGraphName());
    Iterator<GraphNode> pipelineIterator = graph.getLinearGraphIterator();
    while (pipelineIterator.hasNext()) {
      GraphNode node = pipelineIterator.next();
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
  public void setArtifactNeeded(boolean artifactNeeded) {
    this.artifactNeeded = artifactNeeded;
  }

  @Override
  @SchemaIgnore
  @Deprecated
  public String getDeploymentType() {
    return deploymentType;
  }

  @Override
  @Deprecated
  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

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

  @SchemaIgnore
  @Override
  public List<String> getKeywords() {
    return super.getKeywords();
  }

  public Command cloneInternal() {
    Command clonnedCommand = aCommand().withName(getName()).withGraph(getGraph()).build();
    if (getGraph() == null) {
      clonnedCommand.setCommandUnits(getCommandUnits());
    }
    clonnedCommand.setCommandUnitType(getCommandUnitType());
    return clonnedCommand;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String referenceId;
    private String originEntityId;
    private Graph graph;
    private List<CommandUnit> commandUnits = Lists.newArrayList();
    private String name;
    private CommandExecutionStatus commandExecutionStatus;
    private boolean artifactNeeded;
    private CommandType commandType = CommandType.OTHER;
    private List<Variable> templateVariables = new ArrayList<>();

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

    public Builder withOriginEntityId(String originEntityId) {
      this.originEntityId = originEntityId;
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
      this.commandUnits.addAll(asList(commandUnits));
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
     * @param commandExecutionStatus the execution result
     * @return the builder
     */
    public Builder withExecutionResult(CommandExecutionStatus commandExecutionStatus) {
      this.commandExecutionStatus = commandExecutionStatus;
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
     * With Variables
     */
    public Builder withTemplateVariables(List<Variable> templateVariables) {
      this.templateVariables = templateVariables;
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
          .withExecutionResult(commandExecutionStatus)
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
      command.setOriginEntityId(originEntityId);
      command.setGraph(graph);
      command.setCommandUnits(commandUnits);
      command.setName(name);
      command.setCommandExecutionStatus(commandExecutionStatus);
      command.setArtifactNeeded(artifactNeeded);
      command.setCommandType(commandType);
      command.setTemplateVariables(templateVariables);
      return command;
    }
  }
}
