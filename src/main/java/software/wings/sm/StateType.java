package software.wings.sm;

import static software.wings.sm.StateTypeScope.ORCHESTRATION_STENCILS;
import static software.wings.sm.StateTypeScope.PIPELINE_STENCILS;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.BuildState;
import software.wings.sm.states.EmailState;
import software.wings.sm.states.EnvState;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.PauseState;
import software.wings.sm.states.RepeatState;
import software.wings.sm.states.StartState;
import software.wings.sm.states.StopState;
import software.wings.sm.states.WaitState;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Represents type of state.
 *
 * @author Rishi
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StateType implements StateTypeDescriptor {
  REPEAT(ORCHESTRATION_STENCILS),
  FORK(ORCHESTRATION_STENCILS),

  // STATE_MACHINE(ORCHESTRATION_STENCILS),

  WAIT(ORCHESTRATION_STENCILS),
  PAUSE(ORCHESTRATION_STENCILS),

  // DEPLOY(ORCHESTRATION_STENCILS),
  START(ORCHESTRATION_STENCILS),
  STOP(ORCHESTRATION_STENCILS),
  // RESTART(ORCHESTRATION_STENCILS),

  HTTP(ORCHESTRATION_STENCILS),
  // SPLUNK(ORCHESTRATION_STENCILS), APP_DYNAMICS(ORCHESTRATION_STENCILS),
  EMAIL(ORCHESTRATION_STENCILS),

  BUILD(PIPELINE_STENCILS),
  ENV_STATE(PIPELINE_STENCILS),
  APPROVAL(ORCHESTRATION_STENCILS, PIPELINE_STENCILS);

  private static final String stencilsPath = "/templates/stencils/";
  private static final String jsonSchemaSuffix = "-JSONSchema.json";
  private static final String uiSchemaSuffix = "-UISchema.json";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Object jsonSchema;
  private Object uiSchema;
  private List<StateTypeScope> scopes = new ArrayList<>();

  StateType(StateTypeScope... scopes) {
    this.scopes = Arrays.asList(scopes);
    // this.jsonSchema = loadJsonSchema();
    this.uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      WingsException ex = new WingsException("Error in initializing StateType-" + file, exception);
      logger.error(ex.getMessage(), ex);
      return null;
      // throw ex;
      // TODO - uncomment exception later on
    }
  }

  @Override
  public String getType() {
    return name();
  }

  @Override
  public Object getJsonSchema() {
    return loadJsonSchema();
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public List<StateTypeScope> getScopes() {
    return scopes;
  }

  /*
   * (non-Javadoc)
   *
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
      case BUILD: {
        return new BuildState(id);
      }
      case ENV_STATE: {
        return new EnvState(id);
      }

      case STOP: {
        return new StopState(id);
      }
      case START: {
        return new StartState(id);
      }
      case APPROVAL: {
        return new ApprovalState(id);
      }
      case EMAIL: {
        return new EmailState(id);
      }
      default: { throw new WingsException("State implementation is not supported for " + id); }
    }
  }

  private JsonNode loadJsonSchema() {
    switch (this) {
      case REPEAT: {
        return JsonUtils.jsonSchema(RepeatState.class);
      }
      case FORK: {
        return JsonUtils.jsonSchema(ForkState.class);
      }
      case HTTP: {
        return JsonUtils.jsonSchema(HttpState.class);
      }
      case WAIT: {
        return JsonUtils.jsonSchema(WaitState.class);
      }
      case PAUSE: {
        return JsonUtils.jsonSchema(PauseState.class);
      }
      case BUILD: {
        return JsonUtils.jsonSchema(BuildState.class);
      }
      case ENV_STATE: {
        return JsonUtils.jsonSchema(EnvState.class);
      }
      case STOP: {
        return JsonUtils.jsonSchema(StopState.class);
      }
      case START: {
        return JsonUtils.jsonSchema(StartState.class);
      }
      case APPROVAL: {
        return JsonUtils.jsonSchema(ApprovalState.class);
      }
      case EMAIL: {
        return JsonUtils.jsonSchema(EmailState.class);
      }
      default: { throw new WingsException("State implementation is not supported for type " + this); }
    }
  }
}
