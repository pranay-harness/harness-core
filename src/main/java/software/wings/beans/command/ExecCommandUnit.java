package software.wings.beans.command;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static jersey.repackaged.com.google.common.collect.ImmutableMap.of;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static software.wings.beans.command.CommandUnitType.EXEC;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by anubhaw on 5/25/16.
 */
public class ExecCommandUnit extends CommandUnit {
  @JsonIgnore @Transient private final Configuration cfg = new Configuration(VERSION_2_3_23);
  @Attributes(title = "Working Directory") @NotEmpty private String commandPath;
  @Attributes(title = "Command") @NotEmpty private String commandString;
  @Attributes(title = "Tail Files?") private boolean tailFiles;

  @Attributes(title = "Files and Patterns") private List<TailFilePatternEntry> tailPatterns;

  @SchemaIgnore private String preparedCommand;

  /**
   * Instantiates a new exec command unit.
   */
  public ExecCommandUnit() {
    super(EXEC);
  }

  @Override
  public List<String> prepare(String activityId, String executionStagingDir, String launcherScriptFileName,
      String prefix) throws IOException, TemplateException {
    String commandFileName = "wings" + DigestUtils.md5Hex(prefix + getName() + activityId);
    String commandFile = new File(System.getProperty("java.io.tmpdir"), commandFileName).getAbsolutePath();
    String commandDir = isNotBlank(commandPath) ? "'" + commandPath.trim() + "'" : "";

    try (OutputStreamWriter fileWriter =
             new OutputStreamWriter(new FileOutputStream(commandFile), StandardCharsets.UTF_8)) {
      CharStreams.asWriter(fileWriter).append(commandString).close();
      preparedCommand = executionStagingDir + "/" + launcherScriptFileName + " " + commandDir + " " + commandFileName;
    }

    List<String> returnValue = Lists.newArrayList(commandFile);

    if (tailFiles && !isEmpty(tailPatterns)) {
      cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/commandtemplates"));
      String tailWrapperFileName = "wingstailwrapper" + DigestUtils.md5Hex(prefix + getName() + activityId);
      String tailWrapperFile = new File(System.getProperty("java.io.tmpdir"), tailWrapperFileName).getAbsolutePath();
      try (OutputStreamWriter fileWriter =
               new OutputStreamWriter(new FileOutputStream(tailWrapperFile), StandardCharsets.UTF_8)) {
        cfg.getTemplate("tailwrapper.ftl")
            .process(
                of("tailPatterns", tailPatterns, "executionId", activityId, "executionStagingDir", executionStagingDir),
                fileWriter);
      }
      returnValue.add(tailWrapperFile);
      returnValue.add(tailWrapperFile);
      preparedCommand = executionStagingDir + "/" + launcherScriptFileName + " " + commandDir + " "
          + tailWrapperFileName + " " + commandFileName;
    }

    return returnValue;
  }

  @Override
  public ExecutionResult execute(CommandExecutionContext context) {
    return context.executeCommandString(preparedCommand);
  }

  /**
   * Getter for property 'commandPath'.
   *
   * @return Value for property 'commandPath'.
   */
  public String getCommandPath() {
    return commandPath;
  }

  /**
   * Setter for property 'commandPath'.
   *
   * @param commandPath Value to set for property 'commandPath'.
   */
  public void setCommandPath(String commandPath) {
    this.commandPath = commandPath;
  }

  /**
   * Gets command string.
   *
   * @return the command string
   */
  public String getCommandString() {
    return commandString;
  }

  /**
   * Sets command string.
   *
   * @param commandString the command string
   */
  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }

  /**
   * Getter for property 'preparedCommand'.
   *
   * @return Value for property 'preparedCommand'.
   */
  public String getPreparedCommand() {
    return preparedCommand;
  }

  /**
   * Setter for property 'preparedCommand'.
   *
   * @param preparedCommand Value to set for property 'preparedCommand'.
   */
  public void setPreparedCommand(String preparedCommand) {
    this.preparedCommand = preparedCommand;
  }

  public boolean isTailFiles() {
    return tailFiles;
  }

  public void setTailFiles(boolean tailFiles) {
    this.tailFiles = tailFiles;
  }

  public List<TailFilePatternEntry> getTailPatterns() {
    return tailPatterns;
  }

  public void setTailPatterns(List<TailFilePatternEntry> tailPatterns) {
    this.tailPatterns = tailPatterns;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String commandPath;
    private String commandString;
    private String name;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;
    private boolean artifactNeeded = false;

    private Builder() {}

    /**
     * An exec command unit builder.
     *
     * @return the builder
     */
    public static Builder anExecCommandUnit() {
      return new Builder();
    }

    /**
     * With command path builder.
     *
     * @param commandPath the command path
     * @return the builder
     */
    public Builder withCommandPath(String commandPath) {
      this.commandPath = commandPath;
      return this;
    }

    /**
     * With command string builder.
     *
     * @param commandString the command string
     * @return the builder
     */
    public Builder withCommandString(String commandString) {
      this.commandString = commandString;
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
     * With command unit type builder.
     *
     * @param commandUnitType the command unit type
     * @return the builder
     */
    public Builder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
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
      return anExecCommandUnit()
          .withCommandPath(commandPath)
          .withCommandString(commandString)
          .withName(name)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult)
          .withArtifactNeeded(artifactNeeded);
    }

    /**
     * Build exec command unit.
     *
     * @return the exec command unit
     */
    public ExecCommandUnit build() {
      ExecCommandUnit execCommandUnit = new ExecCommandUnit();
      execCommandUnit.setCommandPath(commandPath);
      execCommandUnit.setCommandString(commandString);
      execCommandUnit.setName(name);
      execCommandUnit.setCommandUnitType(commandUnitType);
      execCommandUnit.setExecutionResult(executionResult);
      execCommandUnit.setArtifactNeeded(artifactNeeded);
      return execCommandUnit;
    }
  }
}
