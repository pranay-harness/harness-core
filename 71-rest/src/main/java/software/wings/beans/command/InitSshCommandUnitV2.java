package software.wings.beans.command;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.Util.escapifyString;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.harness.delegate.command.CommandExecutionResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.Constants;
import software.wings.utils.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class InitSshCommandUnitV2 extends SshCommandUnit {
  /**
   * The constant INITIALIZE_UNIT.
   */
  private static final transient String INITIALIZE_UNIT = "Initialize";
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);
  static {
    try {
      StringTemplateLoader stringLoader = new StringTemplateLoader();

      InputStream execLauncherInputStream =
          InitSshCommandUnitV2.class.getClassLoader().getResourceAsStream("commandtemplates/execlauncherv2.sh.ftl");
      if (execLauncherInputStream == null) {
        throw new RuntimeException("execlauncherv2.sh.ftl file is missing.");
      }

      stringLoader.putTemplate("execlauncherv2.sh.ftl",
          convertToUnixStyleLineEndings(IOUtils.toString(execLauncherInputStream, StandardCharsets.UTF_8)));

      InputStream tailWrapperInputStream =
          InitSshCommandUnitV2.class.getClassLoader().getResourceAsStream("commandtemplates/tailwrapperv2.sh.ftl");
      if (tailWrapperInputStream == null) {
        throw new RuntimeException("tailwrapperv2.sh.ftl file is missing.");
      }

      stringLoader.putTemplate("tailwrapperv2.sh.ftl",
          convertToUnixStyleLineEndings(IOUtils.toString(tailWrapperInputStream, StandardCharsets.UTF_8)));
      cfg.setTemplateLoader(stringLoader);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load and parse commandtemplates ", e);
    }
  }

  @JsonIgnore @SchemaIgnore @Transient private Command command;

  @JsonIgnore @SchemaIgnore private String activityId;

  @JsonIgnore @Transient @SchemaIgnore private Map<String, String> envVariables = Maps.newHashMap();

  @JsonIgnore @Transient @SchemaIgnore private Map<String, String> safeDisplayEnvVariables = Maps.newHashMap();

  @JsonIgnore @Transient @SchemaIgnore private String preInitCommand;

  @JsonIgnore @Transient @SchemaIgnore private String executionStagingDir;

  @JsonIgnore
  @Transient
  @SchemaIgnore
  protected static final Logger logger = LoggerFactory.getLogger(InitSshCommandUnitV2.class);

  public InitSshCommandUnitV2() {
    super(CommandUnitType.EXEC);
    setName(INITIALIZE_UNIT);
  }

  private static String convertToUnixStyleLineEndings(String input) {
    return input.replace("\r\n", "\n");
  }

  @Override
  protected CommandExecutionResult.CommandExecutionStatus executeInternal(ShellCommandExecutionContext context) {
    activityId = context.getActivityId();
    executionStagingDir = "/tmp/" + activityId;
    preInitCommand = "mkdir -p " + executionStagingDir;
    CommandExecutionResult.CommandExecutionStatus commandExecutionStatus = context.executeCommandString(preInitCommand);

    Validator.notNullCheck("Service Variables", context.getServiceVariables());
    for (Map.Entry<String, String> entry : context.getServiceVariables().entrySet()) {
      envVariables.put(entry.getKey(), escapifyString(entry.getValue()));
    }
    envVariables.put("WINGS_STAGING_PATH", context.getStagingPath());
    envVariables.put("WINGS_RUNTIME_PATH", context.getRuntimePath());
    envVariables.put("WINGS_BACKUP_PATH", context.getBackupPath());
    if (isNotEmpty(context.getArtifactFiles())) {
      String name = context.getArtifactFiles().get(0).getName();
      if (isNotEmpty(name)) {
        envVariables.put("ARTIFACT_FILE_NAME", name);
      }
    } else if (context.getMetadata() != null) {
      String value = context.getMetadata().get(Constants.ARTIFACT_FILE_NAME);
      if (isNotEmpty(value)) {
        envVariables.put("ARTIFACT_FILE_NAME", value);
      }
    }

    Validator.notNullCheck("Safe Display Service Variables", context.getSafeDisplayServiceVariables());
    for (Map.Entry<String, String> entry : context.getSafeDisplayServiceVariables().entrySet()) {
      safeDisplayEnvVariables.put(entry.getKey(), escapifyString(entry.getValue()));
    }
    StringBuffer envVariablesFromHost = new StringBuffer();
    commandExecutionStatus = commandExecutionStatus == CommandExecutionResult.CommandExecutionStatus.SUCCESS
        ? context.executeCommandString("printenv", envVariablesFromHost)
        : commandExecutionStatus;
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(envVariablesFromHost.toString().replaceAll("\\\\", "\\\\\\\\")));
      context.addEnvVariables(
          properties.entrySet().stream().collect(toMap(o -> o.getKey().toString(), o -> o.getValue().toString())));
    } catch (IOException e) {
      logger.error("Error in InitCommandUnit", e);
      commandExecutionStatus = CommandExecutionResult.CommandExecutionStatus.FAILURE;
    }
    try {
      createPreparedCommands(command);
    } catch (IOException | TemplateException e) {
      logger.error("Failed in preparing commands ", e);
    }
    context.addEnvVariables(envVariables);
    return commandExecutionStatus;
  }

  private String getInitCommand(String scriptWorkingDirectory, boolean includeTailFunctions)
      throws IOException, TemplateException {
    try (StringWriter stringWriter = new StringWriter()) {
      Map<String, Object> templateParams = ImmutableMap.<String, Object>builder()
                                               .put("executionId", activityId)
                                               .put("executionStagingDir", executionStagingDir)
                                               .put("envVariables", envVariables)
                                               .put("safeEnvVariables", safeDisplayEnvVariables)
                                               .put("scriptWorkingDirectory", scriptWorkingDirectory)
                                               .put("includeTailFunctions", includeTailFunctions)
                                               .build();
      cfg.getTemplate("execlauncherv2.sh.ftl").process(templateParams, stringWriter);
      return stringWriter.toString();
    }
  }

  private void createPreparedCommands(Command command) throws IOException, TemplateException {
    for (CommandUnit unit : command.getCommandUnits()) {
      if (unit instanceof Command) {
        createPreparedCommands((Command) unit);
      } else {
        if (unit instanceof ExecCommandUnit) {
          ExecCommandUnit execCommandUnit = (ExecCommandUnit) unit;
          String commandDir =
              isNotBlank(execCommandUnit.getCommandPath()) ? "'" + execCommandUnit.getCommandPath().trim() + "'" : "";
          String commandString = execCommandUnit.getCommandString();
          boolean includeTailFunctions = isNotEmpty(execCommandUnit.getTailPatterns())
              || StringUtils.contains(commandString, "harness_utils_start_tail_log_verification")
              || StringUtils.contains(commandString, "harness_utils_wait_for_tail_log_verification");
          StringBuilder preparedCommand = new StringBuilder(getInitCommand(commandDir, includeTailFunctions));
          if (isEmpty(execCommandUnit.getTailPatterns())) {
            preparedCommand.append(commandString);
          } else {
            try (StringWriter stringWriter = new StringWriter()) {
              Map<String, Object> templateParams = ImmutableMap.<String, Object>builder()
                                                       .put("tailPatterns", execCommandUnit.getTailPatterns())
                                                       .put("executionId", activityId)
                                                       .put("executionStagingDir", executionStagingDir)
                                                       .put("commandString", commandString)
                                                       .build();
              cfg.getTemplate("tailwrapperv2.sh.ftl").process(templateParams, stringWriter);
              preparedCommand.append(' ').append(stringWriter.toString());
            }
          }

          execCommandUnit.setPreparedCommand(preparedCommand.toString());
        }
      }
    }
  }

  /**
   * Sets command.
   *
   * @param command the command
   */
  public void setCommand(Command command) {
    this.command = command;
  }
}
