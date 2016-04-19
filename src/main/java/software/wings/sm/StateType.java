/**
 *
 */
package software.wings.sm;

import java.net.URL;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import software.wings.common.JsonUtils;
import software.wings.exception.WingsException;

/**
 * @author Rishi
 *
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

  StateType() {
    this.jsonSchema = readResource(stencilsPath + name() + jsonSchemaSuffix);
    this.uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
  }

  private Object jsonSchema;
  private Object uiSchema;

  @Override
  public Object getJsonSchema() {
    return jsonSchema;
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public String getType() {
    return name();
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception e) {
      WingsException ex = new WingsException("Error in initializing StateType", e);
      logger.error(ex.getMessage(), ex);
      throw ex;
    }
  }

  private static final String stencilsPath = "/templates/stencils/";
  private static final String jsonSchemaSuffix = "-JSONSchema.json";
  private static final String uiSchemaSuffix = "-UISchema.json";

  private static final Logger logger = LoggerFactory.getLogger(StateType.class);

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
