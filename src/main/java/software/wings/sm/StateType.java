package software.wings.sm;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.JsonUtils;
import software.wings.exception.WingsException;

import java.net.URL;
import java.util.HashMap;

/**
 * Represents type of state.
 * @author Rishi
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StateType implements StateTypeDescriptor {
  REPEAT,
  FORK,
  HTTP,
  WAIT,
  PAUSE,
  START,
  STOP,
  RESTART,
  DEPLOY,
  STATE_MACHINE;

  private static final String stencilsPath = "/templates/stencils/";
  private static final String jsonSchemaSuffix = "-JSONSchema.json";
  private static final String uiSchemaSuffix = "-UISchema.json";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Object jsonSchema;
  private Object uiSchema;

  StateType() {
    this.jsonSchema = readResource(stencilsPath + name() + jsonSchemaSuffix);
    this.uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      WingsException ex = new WingsException("Error in initializing StateType", exception);
      logger.error(ex.getMessage(), ex);
      throw ex;
    }
  }

  @Override
  public String getType() {
    return name();
  }

  @Override
  public Object getJsonSchema() {
    return jsonSchema;
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  /* (non-Javadoc)
   * @see software.wings.sm.StateTypeDescriptor#newInstance(java.lang.String)
   */
  @Override
  public State newInstance(String id) {
    switch (this) {
      case REPEAT: {
        return new RepeatState(id);
      }
      case FORK: {
        return new ForkState(id);
      }
      case HTTP: {
        return new HttpState(id);
      }
      case WAIT: {
        return new WaitState(id);
      }
      case PAUSE: {
        return new PauseState(id);
      }
      default: { throw new WingsException("newInstance is not supported"); }
    }
  }
}
