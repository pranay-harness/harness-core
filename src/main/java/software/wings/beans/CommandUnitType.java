package software.wings.beans;

import static org.joor.Reflect.on;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import software.wings.exception.WingsException;
import software.wings.stencils.OverridingStencil;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.HashMap;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 6/2/16.
 */
public enum CommandUnitType implements CommandUnitDescriptor {
  /**
   * Exec command unit type.
   */
  EXEC(ExecCommandUnit.class),

  /**
   * Scp command unit type.
   */
  SCP(ScpCommandUnit.class),

  /**
   * Command command unit type.
   */
  COMMAND(Command.class),

  /**
   * Setup env command unit type.
   */
  SETUP_ENV(SetupEnvCommandUnit.class),
  /**
   * Tail log command unit type.
   */
  TAIL_LOG(TailLogCommandUnit.class),
  /**
   * Process check command unit type.
   */
  PROCESS_CHECK(ProcessCheckCommandUnit.class),

  /**
   * Copy artifact command unit type.
   */
  COPY_ARTIFACT(CopyArtifactCommandUnit.class),

  /**
   * Copy platform command unit type.
   */
  COPY_APP_CONTAINER(CopyAppContainerCommandUnit.class);

  private static final String stencilsPath = "/templates/commandstencils/";
  private static final String uiSchemaSuffix = "-UISchema.json";

  private Object uiSchema;
  private JsonNode jsonSchema;

  @JsonIgnore private Class<? extends CommandUnit> commandUnitClass;

  /**
   * Instantiates a new command unit type.
   *
   * @param commandUnitClass the command unit class
   */
  CommandUnitType(Class<? extends CommandUnit> commandUnitClass) {
    this.commandUnitClass = commandUnitClass;
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
    return name();
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
}
