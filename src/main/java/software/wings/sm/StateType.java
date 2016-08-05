package software.wings.sm;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static org.joor.Reflect.on;
import static software.wings.sm.StateTypeScope.ORCHESTRATION_STENCILS;
import static software.wings.sm.StateTypeScope.PIPELINE_STENCILS;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;
import software.wings.sm.states.AppDynamicsState;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.BuildState;
import software.wings.sm.states.CommandState;
import software.wings.sm.states.EmailState;
import software.wings.sm.states.EnvState;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.PauseState;
import software.wings.sm.states.RepeatState;
import software.wings.sm.states.SplunkState;
import software.wings.sm.states.WaitState;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;
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
  /**
   * Repeat state type.
   */
  REPEAT(RepeatState.class, StencilCategory.CONTROLS, ORCHESTRATION_STENCILS),

  /**
   * Fork state type.
   */
  FORK(ForkState.class, StencilCategory.CONTROLS, ORCHESTRATION_STENCILS),

  // STATE_MACHINE(ORCHESTRATION_STENCILS),

  /**
   * Wait state type.
   */
  WAIT(WaitState.class, StencilCategory.CONTROLS, ORCHESTRATION_STENCILS),

  /**
   * Pause state type.
   */
  PAUSE(PauseState.class, StencilCategory.CONTROLS, ORCHESTRATION_STENCILS),

  /**
   * Http state type.
   */
  HTTP(HttpState.class, StencilCategory.COMMONS, ORCHESTRATION_STENCILS),

  /**
   * Splunk state type.
   */
  SPLUNK(SplunkState.class, StencilCategory.VERIFICATIONS, ORCHESTRATION_STENCILS),

  /**
   * App dynamics state type.
   */
  APP_DYNAMICS(AppDynamicsState.class, StencilCategory.VERIFICATIONS, ORCHESTRATION_STENCILS),

  /**
   * Email state type.
   */
  EMAIL(EmailState.class, StencilCategory.COMMONS, ORCHESTRATION_STENCILS),

  /**
   * Build state type.
   */
  BUILD(BuildState.class, StencilCategory.BUILD, PIPELINE_STENCILS),

  /**
   * Env state state type.
   */
  ENV_STATE(EnvState.class, StencilCategory.ENVIRONMENTS, PIPELINE_STENCILS),

  /**
   * Command state type.
   */
  COMMAND(CommandState.class, StencilCategory.COMMANDS, ORCHESTRATION_STENCILS),

  /**
   * Approval state type.
   */
  APPROVAL(ApprovalState.class, StencilCategory.COMMONS, ORCHESTRATION_STENCILS, PIPELINE_STENCILS);

  private static final String stencilsPath = "/templates/stencils/";
  private static final String uiSchemaSuffix = "-UISchema.json";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Class<? extends State> stateClass;
  private Object jsonSchema;
  private Object uiSchema;
  private List<StateTypeScope> scopes = new ArrayList<>();
  private StencilCategory stencilCategory;
  private Integer displayOrder;

  /**
   * Instantiates a new state type.
   *
   * @param stateClass the state class
   * @param scopes     the scopes
   */
  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, StateTypeScope... scopes) {
    this(stateClass, stencilCategory, DEFAULT_DISPLAY_ORDER, scopes);
  }

  /**
   * Instantiates a new state type.
   *
   * @param stateClass   the state class
   * @param displayOrder display order
   * @param scopes       the scopes
   */
  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      StateTypeScope... scopes) {
    this.stateClass = stateClass;
    this.scopes = Arrays.asList(scopes);
    this.jsonSchema = loadJsonSchema();
    this.stencilCategory = stencilCategory;
    this.displayOrder = displayOrder;
    try {
      this.uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
    } catch (Exception e) {
      this.uiSchema = new HashMap<String, Object>();
    }
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new WingsException("Error in initializing StateType-" + file, exception);
    }
  }

  @Override
  public String getType() {
    return name();
  }

  @Override
  public Class<? extends State> getTypeClass() {
    return stateClass;
  }

  @Override
  public JsonNode getJsonSchema() {
    return ((JsonNode) jsonSchema).deepCopy();
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public List<StateTypeScope> getScopes() {
    return scopes;
  }

  @Override
  public String getName() {
    return UPPER_UNDERSCORE.to(UPPER_CAMEL, name());
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.StateTypeDescriptor#newInstance(java.lang.String)
   */
  @Override
  public State newInstance(String id) {
    return on(stateClass).create(id).get();
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return new OverridingStateTypeDescriptor(this);
  }

  private JsonNode loadJsonSchema() {
    return JsonUtils.jsonSchema(stateClass);
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
