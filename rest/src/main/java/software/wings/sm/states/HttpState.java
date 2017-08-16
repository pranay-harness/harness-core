package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static software.wings.api.HttpStateExecutionData.Builder.aHttpStateExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlException.Parsing;
import org.apache.commons.jexl3.JexlException.Property;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HttpStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
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
  private static final Splitter HEADERS_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

  private static final Splitter HEADER_SPLITTER = Splitter.on(":").trimResults();

  private static final Logger logger = LoggerFactory.getLogger(HttpState.class);

  private static final String ASSERTION_ERROR_MSG =
      "Assertion should return true/false (Expression syntax is based on java language).";

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

  @Inject @Transient private transient ActivityService activityService;

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
    ExecutionResponse response = executeInternal(context, activityId);
    return response;
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
    this.body = body;
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
    return asList(url, body, header, assertion);
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
   * @param context the context
   * @return the execution response
   */
  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    String finalUrl = getFinalUrl(context);
    String bodyExpression = null;
    String headerExpression = null;
    String methodExpression = null;
    String assertionExpression = null;
    List<TemplateExpression> templateExpressions = getTemplateExpressions();
    if (getTemplateExpressions() != null && !getTemplateExpressions().isEmpty()) {
      for (TemplateExpression templateExpression : templateExpressions) {
        String fieldName = templateExpression.getFieldName();
        if (fieldName != null) {
          if (fieldName.equals("url")) {
            finalUrl = templateExpression.getExpression();
          } else if (fieldName.equals("header")) {
            headerExpression = templateExpression.getExpression();
          } else if (fieldName.equals("body")) {
            bodyExpression = templateExpression.getExpression();
          } else if (fieldName.equals("method")) {
            methodExpression = templateExpression.getExpression();
          } else if (fieldName.equals("assertion")) {
            assertionExpression = templateExpression.getExpression();
          }
        }
      }
    }
    String evaluatedUrl = context.renderExpression(finalUrl);
    logger.info("evaluatedUrl: {}", evaluatedUrl);
    String evaluatedBody = null;
    try {
      evaluatedBody = getFinalBody(context);
      if (bodyExpression != null) {
        evaluatedBody = bodyExpression;
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (evaluatedBody != null) {
      evaluatedBody = context.renderExpression(evaluatedBody);
      logger.info("evaluatedBody: {}", evaluatedBody);
    }

    String evaluatedHeader = getFinalHeader(context);
    if (evaluatedHeader != null) {
      if (headerExpression != null) {
        evaluatedHeader = headerExpression;
      }
      evaluatedHeader = context.renderExpression(evaluatedHeader);
      logger.info("evaluatedHeader: {}", evaluatedHeader);
    }

    String evaluatedMethod = getFinalMethod(context);
    if (methodExpression != null) {
      evaluatedMethod = context.renderExpression(evaluatedMethod);
    }

    if (assertionExpression != null) {
      assertion = context.renderExpression(assertionExpression);
    }
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    String delegateTaksId = delegateService.queueTask(
        aDelegateTask()
            .withTaskType(getTaskType())
            .withAccountId(((ExecutionContextImpl) context).getApp().getAccountId())
            .withWaitId(activityId)
            .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
            .withParameters(
                new Object[] {evaluatedMethod, evaluatedUrl, evaluatedBody, evaluatedHeader, socketTimeoutMillis})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .build());

    HttpStateExecutionData.Builder executionDataBuilder =
        aHttpStateExecutionData().withHttpUrl(evaluatedUrl).withHttpMethod(evaluatedMethod);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withStateExecutionData(executionDataBuilder.build())
        .withDelegateTaskId(delegateTaksId)
        .build();
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
    HttpStateExecutionData executionData = (HttpStateExecutionData) response.values().iterator().next();
    String errorMessage = executionData.getErrorMsg();
    executionData.setAssertionStatement(assertion);
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    boolean assertionStatus = true;
    if (StringUtils.isNotBlank(assertion)) {
      // check if the request failed
      if (!executionData.getStatus().equals(ExecutionStatus.ERROR)) {
        try {
          assertionStatus = (boolean) context.evaluateExpression(assertion, executionData);
          logger.info("assertion status: {}", assertionStatus);

        } catch (ClassCastException e) {
          logger.error("Invalid assertion " + e.getMessage(), e);
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
    ExecutionResponse executionResponse = new ExecutionResponse();
    executionResponse.setExecutionStatus(executionStatus);

    executionData.setAssertionStatus(executionStatus.name());
    executionResponse.setStateExecutionData(executionData);
    executionResponse.setErrorMessage(errorMessage);

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

    Activity.Builder activityBuilder =
        anActivity()
            .withAppId(app.getUuid())
            .withApplicationName(app.getName())
            .withEnvironmentId(env.getUuid())
            .withEnvironmentName(env.getName())
            .withEnvironmentType(env.getEnvironmentType())
            .withCommandName(getName())
            .withType(Type.Verification)
            .withWorkflowType(executionContext.getWorkflowType())
            .withWorkflowExecutionName(executionContext.getWorkflowExecutionName())
            .withStateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .withStateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .withCommandType(getStateType())
            .withWorkflowExecutionId(executionContext.getWorkflowExecutionId());

    if (instanceElement != null && instanceElement.getServiceTemplateElement() != null) {
      activityBuilder.withServiceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .withServiceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .withServiceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .withServiceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .withServiceInstanceId(instanceElement.getUuid())
          .withHostName(instanceElement.getHost().getHostName());
    }

    return activityService.save(activityBuilder.build()).getUuid();
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
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Couldn't url-encode " + queryString);
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

    /**
     * Do not instantiate Builder.
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
          .withSocketTimeoutMillis(socketTimeoutMillis);
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
      return httpState;
    }
  }
}
