package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.beans.EntityType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a state object.
 *
 * @author Rishi
 */
public abstract class State {
  @SchemaIgnore private String id;

  @SchemaIgnore private String parentId;

  @SchemaIgnore private String name;

  @SchemaIgnore private ContextElementType requiredContextElementType;

  @SchemaIgnore private String stateType;

  @SchemaIgnore private boolean rollback;

  @SchemaIgnore private Integer waitInterval;

  @SchemaIgnore private Integer timeoutMillis;

  @SchemaIgnore private boolean ignoreFailure;

  @SchemaIgnore private List<TemplateExpression> templateExpressions;

  @SchemaIgnore private boolean executeWithPreviousSteps;

  @SchemaIgnore private String templateUuid;

  @SchemaIgnore private String templateVersion;

  @SchemaIgnore protected List<Variable> templateVariables = new ArrayList<>();

  @SchemaIgnore
  public String getTemplateUuid() {
    return templateUuid;
  }

  public void setTemplateUuid(String templateUuid) {
    this.templateUuid = templateUuid;
  }

  public State() {}
  /**
   * Instantiates a new state.
   *
   * @param name      the name
   * @param stateType the state type
   */
  public State(String name, String stateType) {
    this.name = name;
    this.stateType = stateType;
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  @SchemaIgnore
  public String getId() {
    return id;
  }

  /**
   * Sets id.
   *
   * @param id the id
   */
  @SchemaIgnore
  public void setId(String id) {
    this.id = id;
  }

  @SchemaIgnore
  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  @SchemaIgnore
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  @SchemaIgnore
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets required context element type.
   *
   * @return the required context element type
   */
  @SchemaIgnore
  public ContextElementType getRequiredContextElementType() {
    return requiredContextElementType;
  }

  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    return null;
  }

  /**
   * Sets required context element type.
   *
   * @param requiredContextElementType the required context element type
   */
  @SchemaIgnore
  public void setRequiredContextElementType(ContextElementType requiredContextElementType) {
    this.requiredContextElementType = requiredContextElementType;
  }

  /**
   * Gets state type.
   *
   * @return the state type
   */
  @SchemaIgnore
  public String getStateType() {
    return stateType;
  }

  @SchemaIgnore
  public List<TemplateExpression> getTemplateExpressions() {
    return templateExpressions;
  }

  public void setTemplateExpressions(List<TemplateExpression> templateExpressions) {
    this.templateExpressions = templateExpressions;
  }

  @SchemaIgnore
  public boolean isExecuteWithPreviousSteps() {
    return executeWithPreviousSteps;
  }

  public void setExecuteWithPreviousSteps(boolean executeWithPreviousSteps) {
    this.executeWithPreviousSteps = executeWithPreviousSteps;
  }

  /**
   * Sets state type.
   *
   * @param stateType the state type
   */
  @SchemaIgnore
  public void setStateType(String stateType) {
    this.stateType = stateType;
  }

  @SchemaIgnore
  public boolean isRollback() {
    return rollback;
  }

  @SchemaIgnore
  public void setRollback(boolean rollback) {
    this.rollback = rollback;
  }

  @SchemaIgnore
  @Attributes(title = "Wait interval before execution(in seconds)")
  public Integer getWaitInterval() {
    return waitInterval;
  }

  @SchemaIgnore
  public void setWaitInterval(Integer waitInterval) {
    this.waitInterval = waitInterval;
  }

  @SchemaIgnore
  public boolean isIgnoreFailure() {
    return ignoreFailure;
  }

  public void setIgnoreFailure(boolean ignoreFailure) {
    this.ignoreFailure = ignoreFailure;
  }

  @SchemaIgnore
  public String getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(String templateVersion) {
    this.templateVersion = templateVersion;
  }

  /**
   * Execute.
   *
   * @param context the context
   * @return the execution response
   */
  public abstract ExecutionResponse execute(ExecutionContext context);

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  public abstract void handleAbortEvent(ExecutionContext context);

  /**
   * Gets required execution argument types.
   *
   * @return the required execution argument types
   */
  @SchemaIgnore
  public List<EntityType> getRequiredExecutionArgumentTypes() {
    return null;
  }

  @Attributes(title = "Timeout (Milli-seconds)")
  @SchemaIgnore
  public Integer getTimeoutMillis() {
    return timeoutMillis;
  }

  public void setTimeoutMillis(Integer timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "State [name=" + name + ", stateType=" + stateType + "]";
  }

  /**
   * Callback for handing responses from states that this state was waiting on.
   *
   * @param context  Context of execution.
   * @param response map of responses this state was waiting on.
   * @return Response from handling this state.
   */
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    return new ExecutionResponse();
  }

  /**
   * Resolve properties.
   */
  public void resolveProperties() {}

  public Map<String, String> validateFields() {
    return null;
  }

  /**
   * Resolves parent template fields
   */
  public Map<String, String> parentTemplateFields(String fieldName) {
    return null;
  }

  @SchemaIgnore
  public List<Variable> getTemplateVariables() {
    return templateVariables;
  }

  public void setTemplateVariables(List<Variable> templateVariables) {
    this.templateVariables = templateVariables;
  }

  public Map<String, Object> obtainVariableMap(List<Variable> variables) {
    if (isEmpty(variables)) {
      return null;
    }
    return variables.stream()
        .filter(variable -> variable.getName() != null && variable.getValue() != null)
        .collect(Collectors.toMap(Variable::getName, Variable::getValue));
  }
}
