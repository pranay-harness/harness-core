package software.wings.beans.command;

import static org.joor.Reflect.on;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.harness.exception.WingsException;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 6/2/16.
 */
public enum CommandUnitType implements CommandUnitDescriptor {
  EXEC(ExecCommandUnit.class, "Exec", StencilCategory.SCRIPTS, DEFAULT_DISPLAY_ORDER),
  SCP(ScpCommandUnit.class, "Copy", StencilCategory.COPY, DEFAULT_DISPLAY_ORDER),
  COPY_CONFIGS(CopyConfigCommandUnit.class, "Copy Configs", StencilCategory.COPY, DEFAULT_DISPLAY_ORDER),
  COMMAND(Command.class, "Command", StencilCategory.COMMANDS, DEFAULT_DISPLAY_ORDER),
  SETUP_ENV(SetupEnvCommandUnit.class, "Setup Env", StencilCategory.SCRIPTS, DEFAULT_DISPLAY_ORDER),
  DOCKER_START(DockerStartCommandUnit.class, "Docker Start", StencilCategory.SCRIPTS, DEFAULT_DISPLAY_ORDER),
  DOCKER_STOP(DockerStopCommandUnit.class, "Docker Stop", StencilCategory.SCRIPTS, DEFAULT_DISPLAY_ORDER),
  PROCESS_CHECK_RUNNING(
      ProcessCheckRunningCommandUnit.class, "Process Running", StencilCategory.VERIFICATIONS, DEFAULT_DISPLAY_ORDER),
  PROCESS_CHECK_STOPPED(
      ProcessCheckStoppedCommandUnit.class, "Process Stopped", StencilCategory.VERIFICATIONS, DEFAULT_DISPLAY_ORDER),
  PORT_CHECK_CLEARED(
      PortCheckClearedCommandUnit.class, "Port Cleared", StencilCategory.VERIFICATIONS, DEFAULT_DISPLAY_ORDER),
  PORT_CHECK_LISTENING(
      PortCheckListeningCommandUnit.class, "Port Listening", StencilCategory.VERIFICATIONS, DEFAULT_DISPLAY_ORDER),
  CODE_DEPLOY(CodeDeployCommandUnit.class, "Amazon Code Deploy", StencilCategory.COMMANDS, DEFAULT_DISPLAY_ORDER),
  AWS_LAMBDA(AwsLambdaCommandUnit.class, "AWS Lambda", StencilCategory.COMMANDS, DEFAULT_DISPLAY_ORDER),
  AWS_AMI(AmiCommandUnit.class, "AWS AMI", StencilCategory.COMMANDS, DEFAULT_DISPLAY_ORDER),
  ECS_SETUP(EcsSetupCommandUnit.class, "Setup ECS Service", StencilCategory.CONTAINERS, DEFAULT_DISPLAY_ORDER),
  ECS_SETUP_DAEMON_SCHEDULING_TYPE(
      EcsSetupCommandUnit.class, "Setup ECS Daemon Service", StencilCategory.CONTAINERS, DEFAULT_DISPLAY_ORDER),
  KUBERNETES_SETUP(
      KubernetesSetupCommandUnit.class, "Setup Kubernetes Service", StencilCategory.CONTAINERS, DEFAULT_DISPLAY_ORDER),
  RESIZE(ResizeCommandUnit.class, "Resize ECS Service", StencilCategory.CONTAINERS, DEFAULT_DISPLAY_ORDER),
  RESIZE_KUBERNETES(KubernetesResizeCommandUnit.class, "Resize Kubernetes Service", StencilCategory.CONTAINERS,
      DEFAULT_DISPLAY_ORDER),
  DOWNLOAD_ARTIFACT(
      DownloadArtifactCommandUnit.class, "Download Artifact", StencilCategory.SCRIPTS, DEFAULT_DISPLAY_ORDER);

  private static final String stencilsPath = "/templates/commandstencils/";
  private static final String uiSchemaSuffix = "-UISchema.json";

  private Object uiSchema;
  private JsonNode jsonSchema;

  @JsonIgnore private Class<? extends CommandUnit> commandUnitClass;
  @JsonIgnore private String name;

  private StencilCategory stencilCategory;
  private Integer displayOrder;

  /**
   * Instantiates a new command unit type.
   *
   * @param commandUnitClass the command unit class
   * @param name
   */
  CommandUnitType(Class<? extends CommandUnit> commandUnitClass, String name, StencilCategory stencilCategory,
      Integer displayOrder) {
    this.commandUnitClass = commandUnitClass;
    this.name = name;
    this.stencilCategory = stencilCategory;
    this.displayOrder = displayOrder;
    try {
      uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
    } catch (Exception e) {
      uiSchema = new HashMap<String, String>();
    }
  }

  @Override
  public String getType() {
    return name();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public JsonNode getJsonSchema() {
    return Optional.ofNullable(jsonSchema).orElse(jsonSchema = JsonUtils.jsonSchema(commandUnitClass)).deepCopy();
  }

  /**
   * Gets command unit class.
   *
   * @return the command unit class
   */
  @Override
  public Class<? extends CommandUnit> getTypeClass() {
    return commandUnitClass;
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return new OverridingCommandUnitDescriptor(this);
  }

  @Override
  public CommandUnit newInstance(String id) {
    return on(commandUnitClass).create().get();
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new WingsException("Error in initializing CommandUnitType-" + file, exception);
    }
  }

  @Override
  public StencilCategory getStencilCategory() {
    return stencilCategory;
  }

  @Override
  public Integer getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public boolean matches(Object context) {
    return true;
  }
}
