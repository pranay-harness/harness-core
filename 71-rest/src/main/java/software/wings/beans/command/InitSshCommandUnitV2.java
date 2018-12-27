package software.wings.beans.command;

import static com.google.common.collect.ImmutableMap.of;
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
import org.apache.commons.io.IOUtils;
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
  public static final transient String INITIALIZE_UNIT = "Initialize";
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);
  static {
    try {
      StringTemplateLoader stringLoader = new StringTemplateLoader();

      InputStream execLauncherInputStream =
          InitSshCommandUnitV2.class.getClassLoader().getResourceAsStream("commandtemplates/execlauncherv2.ftl");
      if (execLauncherInputStream == null) {
        throw new RuntimeException("execlauncherv2.ftl file is missing.");
      }

      stringLoader.putTemplate("execlauncherv2.ftl",
          convertToUnixStyleLineEndings(IOUtils.toString(execLauncherInputStream, StandardCharsets.UTF_8)));

      InputStream tailWrapperInputStream =
          InitSshCommandUnitV2.class.getClassLoader().getResourceAsStream("commandtemplates/tailwrapperv2.ftl");
      if (tailWrapperInputStream == null) {
        throw new RuntimeException("tailwrapperv2.ftl file is missing.");
      }

      stringLoader.putTemplate("tailwrapperv2.ftl",
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

  private String getInitCommand(String scriptWorkingDirectory) throws IOException, TemplateException {
    try (StringWriter stringWriter = new StringWriter()) {
      cfg.getTemplate("execlauncherv2.ftl")
          .process(of("envVariables", envVariables, "safeEnvVariables", safeDisplayEnvVariables,
                       "scriptWorkingDirectory", scriptWorkingDirectory),
              stringWriter);
      return stringWriter.toString();
    }
  }

  private void createPreparedCommands(Command command) throws IOException, TemplateException {
    String preparedCommand;
    for (CommandUnit unit : command.getCommandUnits()) {
      if (unit instanceof Command) {
        createPreparedCommands((Command) unit);
      } else {
        if (unit instanceof ExecCommandUnit) {
          String commandDir = isNotBlank(((ExecCommandUnit) unit).getCommandPath())
              ? "'" + ((ExecCommandUnit) unit).getCommandPath().trim() + "'"
              : "";
          if (isEmpty(((ExecCommandUnit) unit).getTailPatterns())) {
            preparedCommand = getInitCommand(commandDir) + ((ExecCommandUnit) unit).getCommandString();
          } else {
            preparedCommand = getInitCommand(commandDir);
            try (StringWriter stringWriter = new StringWriter()) {
              cfg.getTemplate("tailwrapperv2.ftl")
                  .process(ImmutableMap.of("tailPatterns", ((ExecCommandUnit) unit).getTailPatterns(), "executionId",
                               activityId, "executionStagingDir", executionStagingDir, "commandString",
                               ((ExecCommandUnit) unit).getCommandString()),
                      stringWriter);
              preparedCommand = preparedCommand + " " + stringWriter.toString();
            }
          }

          ((ExecCommandUnit) unit).setPreparedCommand(preparedCommand);
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
