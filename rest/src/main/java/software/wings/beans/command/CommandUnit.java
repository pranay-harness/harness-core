package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.github.reinert.jjschema.SchemaIgnore;
import freemarker.template.TemplateException;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by peeyushaggarwal on 11/14/16.
 */
@JsonTypeInfo(use = Id.NAME, property = "commandUnitType")
public interface CommandUnit {
  /**
   * Prepare list.
   *
   * @param activityId             the activity id
   * @param executionStagingDir    the execution staging dir
   * @param launcherScriptFileName the launcher script file name
   * @param prefix                 the prefix
   * @return the list
   * @throws IOException       the io exception
   * @throws TemplateException the template exception
   */
  default List
    <String> prepare(String activityId, String executionStagingDir, String launcherScriptFileName, String prefix)
        throws IOException, TemplateException {
      return Collections.emptyList();
    }

    ExecutionResult execute(CommandExecutionContext context);

    @SchemaIgnore CommandUnitType getCommandUnitType();

    void setCommandUnitType(CommandUnitType commandUnitType);

    @SchemaIgnore ExecutionResult getExecutionResult();

    void setExecutionResult(ExecutionResult executionResult);

    @SchemaIgnore String getName();

    @SchemaIgnore void setName(String name);

    @SchemaIgnore boolean isArtifactNeeded();

    void setArtifactNeeded(boolean artifactNeeded);

    /**
     * Gets command execution timeout.
     *
     * @return the command execution timeout
     */
    @SchemaIgnore
    @JsonIgnore
    default int getCommandExecutionTimeout() {
      return 10 * 60 * 1000;
    }
}
