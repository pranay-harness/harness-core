package software.wings.beans.command;

import static org.joor.Reflect.on;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import software.wings.exception.WingsException;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.HashMap;

/**
 * Created by peeyushaggarwal on 6/2/16.
 */
public enum CommandUnitType implements CommandUnitDescriptor {
  /**
   * Exec command unit type.
   */
  EXEC(ExecCommandUnit.class, "Exec", StencilCategory.SCRIPTS, DEFAULT_DISPLAY_ORDER),

  /**
   * Scp command unit type.
   */
  SCP(ScpCommandUnit.class, "Copy", StencilCategory.COPY, DEFAULT_DISPLAY_ORDER),

  /**
   * Command command unit type.
   */
  COMMAND(Command.class, "Command", StencilCategory.COMMANDS, DEFAULT_DISPLAY_ORDER),

  /**
   * Setup env command unit type.
   */
  SETUP_ENV(SetupEnvCommandUnit.class, "Setup Env", StencilCategory.SCRIPTS,
      DEFAULT_DISPLAY_ORDER), /**
                               * Process check command unit type.
                               */
  PROCESS_CHECK_RUNNING(ProcessCheckRunningCommandUnit.class, "Process Check Running", StencilCategory.VERIFICATIONS,
      DEFAULT_DISPLAY_ORDER),

  PROCESS_CHECK_STOPPED(ProcessCheckStoppedCommandUnit.class, "Process Check Stopped", StencilCategory.VERIFICATIONS,
      DEFAULT_DISPLAY_ORDER),

  PORT_CHECK_CLEARED(
      PortCheckClearedCommandUnit.class, "Port Check Cleared", StencilCategory.VERIFICATIONS, DEFAULT_DISPLAY_ORDER),

  PORT_CHECK_LISTENING(PortCheckListeningCommandUnit.class, "Port Check Listening", StencilCategory.VERIFICATIONS,
      DEFAULT_DISPLAY_ORDER);

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
  CommandUnitType(Class<? extends CommandUnit> commandUnitClass, String name) {
    this(commandUnitClass, name, StencilCategory.OTHERS, DEFAULT_DISPLAY_ORDER);
  }

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
    jsonSchema = JsonUtils.jsonSchema(commandUnitClass);
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
    return jsonSchema.deepCopy();
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
}
