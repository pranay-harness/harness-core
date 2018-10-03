package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.template.TemplateHelper.convertToVariableMap;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlException.Parsing;
import org.apache.commons.jexl3.JexlException.Property;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HttpStateExecutionData;
import software.wings.api.HttpStateExecutionData.HttpStateExecutionDataBuilder;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.TaskType;
import software.wings.beans.Variable;
import software.wings.common.Constants;
import software.wings.expression.ExpressionEvaluator;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Http state which makes a call to http service.
 *
 * @author Rishi
 */
@Attributes
public class HttpState extends State {
  private static final Logger logger = LoggerFactory.getLogger(HttpState.class);

  private static final String ASSERTION_ERROR_MSG =
      "Assertion should return true/false (Expression syntax is based on java language).";

  public static final String URL_KEY = "url";
  public static final String METHOD_KEY = "method";

  @Attributes(required = true, title = "URL") private String url;
  @Attributes(required = true, enums = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"}, title = "Method")
  @DefaultValue("GET")
  private String method;
  @Attributes(title = "Header") private String header;
  @Attributes(title = "Body") private String body;
  @Attributes(title = "Assertion") private String assertion;
  @SchemaIgnore private int socketTimeoutMillis = 10000;

  @Inject private DelegateService delegateService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject protected ManagerDecryptionService managerDecryptionService;
  @Inject protected SecretManager secretManager;
  @Inject protected ExpressionEvaluator expressionEvaluator;

  @Inject @Transient private transient ActivityService activityService;

  public HttpState() {}

  /**
   * Create a new Http State with given name.
   *
   * @param name name of the state.
   */
  public HttpState(String name) {
    super(name, StateType.HTTP.name());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url.
   *
   * @param url the url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets method.
   *
   * @return the method
   */
  public String getMethod() {
    return method;
  }

  /**
   * Sets method.
   *
   * @param method the method
   */
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   * Gets body.
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets body.
   *
   * @param body the body
   */
  public void setBody(String body) {
    this.body = trim(body);
  }

  /**
   * Gets header.
   *
   * @return the header
   */
  public String getHeader() {
    return header;
  }

  /**
   * Sets header.
   *
   * @param header the header
   */
  public void setHeader(String header) {
    this.header = header;
  }

  /**
   * Gets assertion.
   *
   * @return the assertion
   */
  public String getAssertion() {
    return assertion;
  }

  /**
   * Sets assertion.
   *
   * @param assertion the assertion
   */
  public void setAssertion(String assertion) {
    this.assertion = assertion;
  }

  /**
   * Gets socket timeout millis.
   *
   * @return the socket timeout millis
   */
  @SchemaIgnore
  public int getSocketTimeoutMillis() {
    return socketTimeoutMillis;
  }

  /**
   * Sets socket timeout millis.
   *
   * @param socketTimeoutMillis the socket timeout millis
   */
  @SchemaIgnore
  public void setSocketTimeoutMillis(int socketTimeoutMillis) {
    this.socketTimeoutMillis = socketTimeoutMillis;
  }

  @Override
  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    String resolvedUrl = url;
    String resolvedBody = body;
    String resolvedHeader = header;
    String resolvedAssertion = assertion;
    Map<String, Object> templateVariables = convertToVariableMap(this.getTemplateVariables());
    if (isNotEmpty(templateVariables)) {
      resolvedUrl = fetchTemplatedValue(url);
      resolvedBody = fetchTemplatedValue(body);
      resolvedHeader = fetchTemplatedValue(header);
      resolvedAssertion = fetchTemplatedValue(assertion);
    }
    return asList(resolvedUrl, resolvedBody, resolvedHeader, resolvedAssertion);
  }

  private String fetchTemplatedValue(String fieldName) {
    String templatedField = ExpressionEvaluator.getName(fieldName);
    Map<String, Object> templatedVariables = convertToVariableMap(getTemplateVariables());
    return templatedVariables.containsKey(templatedField) ? (String) templatedVariables.get(templatedField) : fieldName;
  }

  @Attributes(title = "Execute with previous steps")
  public boolean getExecuteWithPreviousSteps() {
    return super.isExecuteWithPreviousSteps();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("url", url)
        .add("method", method)
        .add("header", header)
        .add("body", body)
        .add("assertion", assertion)
        .add("socketTimeoutMillis", socketTimeoutMillis)
        .toString();
  }

  /**
   * Execute internal execution response.
   *
   * @param context the contextS
   * @return the execution response
   */
  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    HttpStateExecutionDataBuilder httpStateExecutionDataBuilder =
        HttpStateExecutionData.builder().variables(convertToVariableMap(getTemplateVariables()));
    String envId = obtainEnvId(context.getContextElement(ContextElementType.STANDARD));

    String finalUrl = getFinalUrl(context);
    String evaluatedUrl = trim(context.renderExpression(finalUrl, httpStateExecutionDataBuilder.build(), null));
    logger.debug("evaluatedUrl: {}", evaluatedUrl);
    String evaluatedBody = null;
    try {
      evaluatedBody = getFinalBody(context);
    } catch (UnsupportedEncodingException e) {
      logger.error("", e);
    }
    if (evaluatedBody != null) {
      evaluatedBody = trim(context.renderExpression(evaluatedBody, httpStateExecutionDataBuilder.build(), null));
      logger.debug("evaluatedBody: {}", evaluatedBody);
    }

    String evaluatedHeader = getFinalHeader(context);
    if (evaluatedHeader != null) {
      evaluatedHeader = trim(context.renderExpression(evaluatedHeader, httpStateExecutionDataBuilder.build(), null));
      logger.debug("evaluatedHeader: {}", evaluatedHeader);
    }
    String evaluatedMethod = getFinalMethod(context);
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();

    Integer stateTimeout = getTimeoutMillis();
    int taskSocketTimeout = socketTimeoutMillis;
    if (stateTimeout != null && taskSocketTimeout > stateTimeout) {
      taskSocketTimeout = stateTimeout - 1000;
    }
    String delegateTaskId = delegateService.queueTask(
        aDelegateTask()
            .withTaskType(getTaskType())
            .withAccountId(((ExecutionContextImpl) context).getApp().getAccountId())
            .withWaitId(activityId)
            .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
            .withParameters(
                new Object[] {evaluatedMethod, evaluatedUrl, evaluatedBody, evaluatedHeader, taskSocketTimeout})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .build());

    HttpStateExecutionDataBuilder executionDataBuilder =
        httpStateExecutionDataBuilder.httpUrl(evaluatedUrl).httpMethod(evaluatedMethod);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withStateExecutionData(executionDataBuilder.build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  private String obtainEnvId(WorkflowStandardParams workflowStandardParams) {
    return (workflowStandardParams == null || workflowStandardParams.getEnv() == null)
        ? null
        : workflowStandardParams.getEnv().getUuid();
  }

  protected String getFinalMethod(ExecutionContext context) {
    return method;
  }

  protected String getFinalHeader(ExecutionContext context) {
    return header;
  }

  protected String getFinalBody(ExecutionContext context) throws UnsupportedEncodingException {
    return body;
  }

  protected String getFinalUrl(ExecutionContext context) {
    return url;
  }

  protected TaskType getTaskType() {
    return TaskType.HTTP;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    NotifyResponseData notifyResponseData = response.values().iterator().next();
    ExecutionResponse executionResponse = new ExecutionResponse();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      executionResponse.setErrorMessage(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else {
      HttpStateExecutionData executionData = (HttpStateExecutionData) context.getStateExecutionData();
      HttpStateExecutionResponse httpStateExecutionResponse = (HttpStateExecutionResponse) notifyResponseData;

      executionData.setHttpResponseCode(httpStateExecutionResponse.getHttpResponseCode());
      executionData.setHttpResponseBody(httpStateExecutionResponse.getHttpResponseBody());
      executionData.setHttpMethod(httpStateExecutionResponse.getHttpMethod());
      executionData.setHttpUrl(httpStateExecutionResponse.getHttpUrl());
      executionData.setStatus(httpStateExecutionResponse.getExecutionStatus());
      executionData.setErrorMsg(httpStateExecutionResponse.getErrorMessage());
      executionData.setDelegateMetaInfo(httpStateExecutionResponse.getDelegateMetaInfo());

      String errorMessage = httpStateExecutionResponse.getErrorMessage();
      executionData.setAssertionStatement(assertion);
      executionData.setTemplateVariable(convertToVariableMap(getTemplateVariables()));
      ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
      boolean assertionStatus = true;
      if (isNotBlank(assertion)) {
        if (!executionData.getStatus().equals(ExecutionStatus.ERROR)) {
          try {
            // check if the request failed
            assertionStatus = (boolean) context.evaluateExpression(assertion, executionData);

            logger.info("assertion status: {}", assertionStatus);
          } catch (ClassCastException e) {
            logger.error("Invalid assertion " + Misc.getMessage(e), e);
            executionData.setErrorMsg(ASSERTION_ERROR_MSG);
          } catch (JexlException e) {
            logger.error("Error in httpStateAssertion", e);
            assertionStatus = false;
            if (e instanceof Parsing) {
              Parsing p = (Parsing) e;
              executionData.setErrorMsg("Parsing error '" + p.getDetail() + "' in assertion.");
            } else if (e instanceof Property) {
              Property pr = (Property) e;
              executionData.setErrorMsg("Unresolvable property '" + pr.getProperty() + "' in assertion.");
            } else {
              executionData.setErrorMsg(getMessage(e));
            }
          } catch (Exception e) {
            logger.error("Error in httpStateAssertion", e);
            executionData.setErrorMsg(getMessage(e));
            assertionStatus = false;
          }
        }
      }
      if (!assertionStatus || executionData.getStatus().equals(ExecutionStatus.ERROR)) {
        executionStatus = ExecutionStatus.FAILED;
      }
      executionResponse.setExecutionStatus(executionStatus);

      executionData.setAssertionStatus(executionStatus.name());
      executionResponse.setStateExecutionData(executionData);
      executionResponse.setErrorMessage(errorMessage);
    }
    String activityId = null;

    for (String key : response.keySet()) {
      activityId = key;
    }

    updateActivityStatus(
        activityId, ((ExecutionContextImpl) context).getApp().getUuid(), executionResponse.getExecutionStatus());

    return executionResponse;
  }

  /**
   * Create activity string.
   *
   * @param executionContext the execution context
   * @return the string
   */
  protected String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .commandName(getName())
                                          .type(Activity.Type.Verification)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .status(ExecutionStatus.RUNNING);

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }
    if (instanceElement != null && instanceElement.getServiceTemplateElement() != null) {
      activityBuilder.serviceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .serviceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .serviceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .serviceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .serviceInstanceId(instanceElement.getUuid())
          .hostName(instanceElement.getHost().getHostName());
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }

  /**
   * Update activity status.
   *
   * @param activityId the activity id
   * @param appId      the app id
   * @param status     the status
   */
  protected void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  protected String urlEncodeString(String queryString) {
    try {
      return URLEncoder.encode(queryString, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Couldn't url-encode " + queryString);
    }
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String url;
    private String method;
    private String header;
    private String body;
    private String assertion;
    private int socketTimeoutMillis = 10000;
    private List<Variable> templateVariables;

    /**
     * Do not instantiate PageResponseBuilder.
     */
    private Builder() {}

    /**
     * A http state builder.
     *
     * @return the builder
     */
    public static Builder aHttpState() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With url builder.
     *
     * @param url the url
     * @return the builder
     */
    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    /**
     * With method builder.
     *
     * @param method the method
     * @return the builder
     */
    public Builder withMethod(String method) {
      this.method = method;
      return this;
    }

    /**
     * With header builder.
     *
     * @param header the header
     * @return the builder
     */
    public Builder withHeader(String header) {
      this.header = header;
      return this;
    }

    /**
     * With body builder.
     *
     * @param body the body
     * @return the builder
     */
    public Builder withBody(String body) {
      this.body = body;
      return this;
    }

    /**
     * With assertion builder.
     *
     * @param assertion the assertion
     * @return the builder
     */
    public Builder withAssertion(String assertion) {
      this.assertion = assertion;
      return this;
    }

    /**
     * With socket timeout millis builder.
     *
     * @param socketTimeoutMillis the socket timeout millis
     * @return the builder
     */
    public Builder withSocketTimeoutMillis(int socketTimeoutMillis) {
      this.socketTimeoutMillis = socketTimeoutMillis;
      return this;
    }

    public Builder withTemplateVariables(List<Variable> templateVariables) {
      this.templateVariables = templateVariables;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHttpState()
          .withName(name)
          .withUrl(url)
          .withMethod(method)
          .withHeader(header)
          .withBody(body)
          .withAssertion(assertion)
          .withSocketTimeoutMillis(socketTimeoutMillis)
          .withTemplateVariables(templateVariables);
    }

    /**
     * Build http state.
     *
     * @return the http state
     */
    public HttpState build() {
      HttpState httpState = new HttpState(name);
      httpState.setUrl(url);
      httpState.setMethod(method);
      httpState.setHeader(header);
      httpState.setBody(body);
      httpState.setAssertion(assertion);
      httpState.setSocketTimeoutMillis(socketTimeoutMillis);
      httpState.setTemplateVariables(templateVariables);
      return httpState;
    }
  }

  @lombok.Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class HttpStateExecutionResponse extends DelegateTaskNotifyResponseData {
    private ExecutionStatus executionStatus;
    private String errorMessage;
    private String httpResponseBody;
    private int httpResponseCode;
    private String httpMethod;
    private String httpUrl;
  }
}
