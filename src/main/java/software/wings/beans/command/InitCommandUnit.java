package software.wings.beans.command;

import static com.google.common.collect.ImmutableMap.of;
import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.codec.digest.DigestUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.service.intfc.ServiceResourceService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
@Attributes(description = "This command unit creates STATING_PATH, RUNTIME_PATH, BACKUP_PATH on the target host")
public class InitCommandUnit extends CommandUnit {
  /**
   * The constant INITIALIZE_UNIT.
   */
  public static final String INITIALIZE_UNIT = "Initialize";
  @JsonIgnore @Transient private final Configuration cfg = new Configuration(VERSION_2_3_23);

  @JsonIgnore @Transient @Inject private ServiceResourceService serviceResourceService;

  @JsonIgnore @SchemaIgnore @Transient private Command command;

  @JsonIgnore @SchemaIgnore private String activityId;

  @JsonIgnore @Transient @SchemaIgnore private Map<String, String> envVariables = Maps.newHashMap();

  @JsonIgnore @Transient @SchemaIgnore private String preInitCommand;

  @JsonIgnore @Transient @SchemaIgnore private String executionStagingDir;

  @JsonIgnore private String launcherScriptFileName;

  /**
   * Instantiates a new Init command unit.
   */
  public InitCommandUnit() {
    super(CommandUnitType.EXEC);
    setName(INITIALIZE_UNIT);
  }

  @Override
  public ExecutionResult execute(CommandExecutionContext context) {
    cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/commandtemplates"));
    activityId = context.getActivityId();
    executionStagingDir = new File("/tmp", activityId).getAbsolutePath();
    preInitCommand = "mkdir -p " + executionStagingDir;
    envVariables.put("WINGS_STAGING_PATH", context.getStagingPath());
    envVariables.put("WINGS_RUNTIME_PATH", context.getRuntimePath());
    envVariables.put("WINGS_BACKUP_PATH", context.getBackupPath());
    envVariables.put("WINGS_SCRIPT_DIR", executionStagingDir);
    launcherScriptFileName = "wingslauncher" + activityId + ".sh";

    ExecutionResult executionResult = context.executeCommandString(preInitCommand);
    try {
      context.copyFiles(executionStagingDir, Collections.singletonList(getLauncherFile()));
    } catch (IOException e) {
      e.printStackTrace();
    } catch (TemplateException e) {
      e.printStackTrace();
    }
    try {
      context.copyFiles(executionStagingDir, getCommandUnitFiles());
    } catch (IOException e) {
      e.printStackTrace();
    }
    context.executeCommandString("chmod 0744 " + executionStagingDir + "/*");
    StringBuffer envVariablesFromHost = new StringBuffer();
    context.executeCommandString("printenv", envVariablesFromHost);
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(envVariablesFromHost.toString()));
      context.addEnvVariables(
          properties.entrySet().stream().collect(toMap(o -> o.getKey().toString(), o -> o.getValue().toString())));
    } catch (IOException e) {
      e.printStackTrace();
    }
    context.addEnvVariables(envVariables);
    return executionResult;
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
      cfg.getTemplate("execlauncher.ftl").process(of("envVariables", envVariables), fileWriter);
    }
    return launcherScript;
  }

  /**
   * Gets command unit files.
   *
   * @return the command unit files
   * @throws IOException the io exception
   */
  private List<String> getCommandUnitFiles() throws IOException {
    return createScripts(command);
  }

  private List<String> createScripts(Command command) throws IOException {
    return createScripts(command, "");
  }

  private List<String> createScripts(Command command, String prefix) throws IOException {
    List<String> files = Lists.newArrayList();
    for (CommandUnit unit : command.getCommandUnits()) {
      if (unit instanceof ExecCommandUnit) {
        ExecCommandUnit execCommandUnit = (ExecCommandUnit) unit;
        String commandFileName = "wings" + DigestUtils.md5Hex(prefix + unit.getName() + activityId);
        String commandFile = new File(System.getProperty("java.io.tmpdir"), commandFileName).getAbsolutePath();
        String commandDir =
            isNotBlank(execCommandUnit.getCommandPath()) ? "'" + execCommandUnit.getCommandPath().trim() + "'" : "";
        try (OutputStreamWriter fileWriter =
                 new OutputStreamWriter(new FileOutputStream(commandFile), StandardCharsets.UTF_8)) {
          CharStreams.asWriter(fileWriter).append(execCommandUnit.getCommandString()).close();
          execCommandUnit.setPreparedCommand(
              executionStagingDir + "/" + launcherScriptFileName + " " + commandDir + " " + commandFileName);
        }
        files.add(commandFile);
      } else if (unit instanceof Command) {
        files.addAll(createScripts((Command) unit, unit.getName()));
      }
    }
    return files;
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
