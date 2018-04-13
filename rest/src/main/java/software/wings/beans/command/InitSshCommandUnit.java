package software.wings.beans.command;

import static com.google.common.collect.ImmutableMap.of;
import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.Util.escapifyString;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.common.Constants;
import software.wings.utils.Validator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by peeyushaggarwal on 7/26/16.
 */
@JsonTypeName("INIT")
public class InitSshCommandUnit extends SshCommandUnit {
  /**
   * The constant INITIALIZE_UNIT.
   */
  public static final transient String INITIALIZE_UNIT = "Initialize";
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);
  static {
    try {
      StringTemplateLoader stringLoader = new StringTemplateLoader();

      InputStream execLauncherInputStream =
          InitSshCommandUnit.class.getClassLoader().getResourceAsStream("commandtemplates/execlauncher.ftl");
      if (execLauncherInputStream == null) {
        throw new RuntimeException("execlauncher.ftl file is missing.");
      }

      stringLoader.putTemplate("execlauncher.ftl",
          convertToUnixStyleLineEndings(IOUtils.toString(execLauncherInputStream, StandardCharsets.UTF_8)));

      InputStream tailWrapperInputStream =
          InitSshCommandUnit.class.getClassLoader().getResourceAsStream("commandtemplates/tailwrapper.ftl");
      if (tailWrapperInputStream == null) {
        throw new RuntimeException("tailwrapper.ftl file is missing.");
      }

      stringLoader.putTemplate("tailwrapper.ftl",
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

  @JsonIgnore private String launcherScriptFileName;

  @JsonIgnore
  @Transient
  @SchemaIgnore
  protected static final Logger logger = LoggerFactory.getLogger(InitSshCommandUnit.class);

  /**
   * Instantiates a new Init command unit.
   */
  public InitSshCommandUnit() {
    super(CommandUnitType.EXEC);
    setName(INITIALIZE_UNIT);
  }

  private static String convertToUnixStyleLineEndings(String input) {
    return input.replace("\r\n", "\n");
  }

  @Override
  protected CommandExecutionStatus executeInternal(ShellCommandExecutionContext context) {
    activityId = context.getActivityId();
    executionStagingDir = "/tmp/" + activityId;
    preInitCommand = "mkdir -p " + executionStagingDir;
    Validator.notNullCheck("Service Variables", context.getServiceVariables());
    for (Map.Entry<String, String> entry : context.getServiceVariables().entrySet()) {
      envVariables.put(entry.getKey(), escapifyString(entry.getValue()));
    }
    envVariables.put("WINGS_STAGING_PATH", context.getStagingPath());
    envVariables.put("WINGS_RUNTIME_PATH", context.getRuntimePath());
    envVariables.put("WINGS_BACKUP_PATH", context.getBackupPath());
    envVariables.put("WINGS_SCRIPT_DIR", executionStagingDir);
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

    launcherScriptFileName = "harnesslauncher" + activityId + ".sh";

    CommandExecutionStatus commandExecutionStatus = context.executeCommandString(preInitCommand);
    try {
      commandExecutionStatus = commandExecutionStatus == CommandExecutionStatus.SUCCESS
          ? context.copyFiles(executionStagingDir, Collections.singletonList(getLauncherFile()))
          : commandExecutionStatus;
    } catch (IOException | TemplateException e) {
      logger.error("Error in InitCommandUnit", e);
    }
    try {
      List<String> commandUnitFiles = getCommandUnitFiles();
      if (isNotEmpty(commandUnitFiles)) {
        commandExecutionStatus = commandExecutionStatus == CommandExecutionStatus.SUCCESS
            ? context.copyFiles(executionStagingDir, commandUnitFiles)
            : commandExecutionStatus;
      }
    } catch (IOException | TemplateException e) {
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      logger.error("Error in InitCommandUnit", e);
    }
    commandExecutionStatus = commandExecutionStatus == CommandExecutionStatus.SUCCESS
        ? context.executeCommandString("chmod 0744 " + executionStagingDir + "/*")
        : commandExecutionStatus;
    StringBuffer envVariablesFromHost = new StringBuffer();
    commandExecutionStatus = commandExecutionStatus == CommandExecutionStatus.SUCCESS
        ? context.executeCommandString("printenv", envVariablesFromHost)
        : commandExecutionStatus;
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(envVariablesFromHost.toString().replaceAll("\\\\", "\\\\\\\\")));
      context.addEnvVariables(
          properties.entrySet().stream().collect(toMap(o -> o.getKey().toString(), o -> o.getValue().toString())));
    } catch (IOException e) {
      logger.error("Error in InitCommandUnit", e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
    }
    context.addEnvVariables(envVariables);
    return commandExecutionStatus;
  }

  /**
   * Gets pre init command.
   *
   * @return the pre init command
   */
  public String getPreInitCommand() {
    return preInitCommand;
  }

  /**
   * Gets launcher file.
   *
   * @return the launcher file
   * @throws IOException       the io exception
   * @throws TemplateException the template exception
   */
  private String getLauncherFile() throws IOException, TemplateException {
    String launcherScript = new File(System.getProperty("java.io.tmpdir"), launcherScriptFileName).getAbsolutePath();
    try (OutputStreamWriter fileWriter =
             new OutputStreamWriter(new FileOutputStream(launcherScript), StandardCharsets.UTF_8)) {
      cfg.getTemplate("execlauncher.ftl")
          .process(of("envVariables", envVariables, "safeEnvVariables", safeDisplayEnvVariables), fileWriter);
    }
    return launcherScript;
  }

  /**
   * Gets command unit files.
   *
   * @return the command unit files
   * @throws IOException the io exception
   */
  private List<String> getCommandUnitFiles() throws IOException, TemplateException {
    return createScripts(command);
  }

  private List<String> createScripts(Command command) throws IOException, TemplateException {
    return createScripts(command, "");
  }

  private List<String> createScripts(Command command, String prefix) throws IOException, TemplateException {
    List<String> files = Lists.newArrayList();
    for (CommandUnit unit : command.getCommandUnits()) {
      if (unit instanceof Command) {
        files.addAll(createScripts((Command) unit, prefix + unit.getName()));
      } else {
        files.addAll(prepare(unit, activityId, executionStagingDir, launcherScriptFileName, prefix));
      }
    }
    return files;
  }

  private static List<String> prepare(CommandUnit commandUnit, String activityId, String executionStagingDir,
      String launcherScriptFileName, String prefix) throws IOException, TemplateException {
    if (commandUnit instanceof ExecCommandUnit) {
      ExecCommandUnit execCommandUnit = (ExecCommandUnit) commandUnit;

      String commandFileName = "harness" + DigestUtils.md5Hex(prefix + execCommandUnit.getName() + activityId);
      String commandFile = new File(System.getProperty("java.io.tmpdir"), commandFileName).getAbsolutePath();
      String commandDir =
          isNotBlank(execCommandUnit.getCommandPath()) ? "-w '" + execCommandUnit.getCommandPath().trim() + "'" : "";
      String preparedCommand = "";

      try (OutputStreamWriter fileWriter =
               new OutputStreamWriter(new FileOutputStream(commandFile), StandardCharsets.UTF_8)) {
        CharStreams.asWriter(fileWriter).append(execCommandUnit.getCommandString()).close();
        preparedCommand = executionStagingDir + "/" + launcherScriptFileName + " " + commandDir + " " + commandFileName;
      }

      List<String> returnValue = Lists.newArrayList(commandFile);

      if (isNotEmpty(execCommandUnit.getTailPatterns())) {
        String tailWrapperFileName =
            "harnesstailwrapper" + DigestUtils.md5Hex(prefix + execCommandUnit.getName() + activityId);
        String tailWrapperFile = new File(System.getProperty("java.io.tmpdir"), tailWrapperFileName).getAbsolutePath();
        try (OutputStreamWriter fileWriter =
                 new OutputStreamWriter(new FileOutputStream(tailWrapperFile), StandardCharsets.UTF_8)) {
          cfg.getTemplate("tailwrapper.ftl")
              .process(ImmutableMap.of("tailPatterns", execCommandUnit.getTailPatterns(), "executionId", activityId,
                           "executionStagingDir", executionStagingDir),
                  fileWriter);
        }
        returnValue.add(tailWrapperFile);
        returnValue.add(tailWrapperFile);
        preparedCommand = executionStagingDir + "/" + launcherScriptFileName + " " + commandDir + " "
            + tailWrapperFileName + " " + commandFileName;
      }
      execCommandUnit.setPreparedCommand(preparedCommand);
      return returnValue;
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Gets execution staging dir.
   *
   * @return the execution staging dir
   */
  public String getExecutionStagingDir() {
    return executionStagingDir;
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
