package software.wings.beans;

import static org.joor.Reflect.on;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 6/2/16.
 */
public enum CommandUnitType {
  /**
   * Exec command unit type.
   */
  EXEC(ExecCommandUnit.class, true), /**
                                      * Command command unit type.
                                      */
  COMMAND(Command.class, true), /**
                                 * Copy artifact command unit type.
                                 */
  COPY_ARTIFACT(CopyArtifactCommandUnit.class, true),

  /**
   * Copy platform command unit type.
   */
  COPY_APP_CONTAINER(CopyAppContainerCommandUnit.class, true);

  private static final String stencilsPath = "/templates/commandstencils/";
  private static final String uiSchemaSuffix = "-UISchema.json";
  private static final List<Map<String, Object>> stencils;

  static {
    ImmutableList.Builder stencilsBuilder = new ImmutableList.Builder();
    for (CommandUnitType value : values()) {
      if (value.isStencil) {
        stencilsBuilder.add(ImmutableMap.of("name", value.name(), "type", value.name(), "uiSchema", value.uiSchema,
            "jsonSchema", JsonUtils.jsonSchema(value.commandUnitClass)));
      }
    }
    stencils = stencilsBuilder.build();
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Object uiSchema;
  private boolean isStencil = false;
  private Class<? extends CommandUnit> commandUnitClass;

  /**
   * Instantiates a new command unit type.
   *
   * @param commandUnitClass the command unit class
   * @param isStencil        the is stencil
   */
  CommandUnitType(Class<? extends CommandUnit> commandUnitClass, boolean isStencil) {
    this.commandUnitClass = commandUnitClass;
    this.isStencil = isStencil;
    if (isStencil) {
      try {
        uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
      } catch (Exception e) {
        uiSchema = new HashMap<String, String>();
      }
    }
  }

  /**
   * Gets stencils.
   *
   * @return the stencils
   */
  public static List<Map<String, Object>> getStencils() {
    return Lists.newArrayList(stencils);
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

  public Class<? extends CommandUnit> getCommandUnitClass() {
    return commandUnitClass;
  }

  /**
   * New instance.
   *
   * @return the command unit
   */
  public CommandUnit newInstance() {
    return on(commandUnitClass).create().get();
  }
}
