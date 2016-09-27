package software.wings.sm.states;

import static com.google.common.base.Ascii.toUpperCase;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static software.wings.beans.Activity.Builder.anActivity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HttpStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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

  @Attributes(required = true, title = "URL") private String url;
  @Attributes(required = true, enums = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"}, title = "Method")
  private String method;
  @Attributes(title = "Header", description = "Content-Type: application/json, Accept: application/json,...")
  private String header;
  @Attributes(title = "Body") private String body;
  @Attributes(title = "Assertion") private String assertion;
  @SchemaIgnore private int socketTimeoutMillis = 10000;

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
    ExecutionResponse response = executeInternal(context);
    updateActivityStatus(activityId, context.getApp().getUuid(), response.getExecutionStatus());
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
  protected ExecutionResponse executeInternal(ExecutionContext context) {
    String errorMessage = null;
    String evaluatedUrl = context.renderExpression(url);
    logger.info("evaluatedUrl: {}", evaluatedUrl);
    String evaluatedBody = body;
    if (evaluatedBody != null) {
      evaluatedBody = context.renderExpression(evaluatedBody);
      logger.info("evaluatedBody: {}", evaluatedBody);
    }

    String evaluatedHeader = header;
    if (evaluatedHeader != null) {
      evaluatedHeader = context.renderExpression(evaluatedHeader);
      logger.info("evaluatedHeader: {}", evaluatedHeader);
    }

    HttpStateExecutionData executionData = new HttpStateExecutionData();
    executionData.setHttpUrl(evaluatedUrl);
    executionData.setHttpMethod(method);
    executionData.setAssertionStatement(assertion);

    SSLContextBuilder builder = new SSLContextBuilder();
    try {
      builder.loadTrustMaterial((x509Certificates, s) -> true);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyStoreException e) {
      e.printStackTrace();
    }
    SSLConnectionSocketFactory sslsf = null;
    try {
      sslsf = new SSLConnectionSocketFactory(builder.build(), (s, sslSession) -> true);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }

    RequestConfig.Builder requestBuilder = RequestConfig.custom();
    requestBuilder = requestBuilder.setConnectTimeout(2000);
    requestBuilder = requestBuilder.setSocketTimeout(socketTimeoutMillis);

    CloseableHttpClient httpclient =
        HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultRequestConfig(requestBuilder.build()).build();

    HttpUriRequest httpUriRequest = null;

    switch (toUpperCase(method)) {
      case "GET": {
        httpUriRequest = new HttpGet(evaluatedUrl);
        break;
      }
      case "POST": {
        HttpPost post = new HttpPost(evaluatedUrl);
        if (evaluatedBody != null) {
          post.setEntity(new StringEntity(evaluatedBody, StandardCharsets.UTF_8));
        }
        httpUriRequest = post;
        break;
      }
      case "PUT": {
        HttpPut put = new HttpPut(evaluatedUrl);
        if (evaluatedBody != null) {
          put.setEntity(new StringEntity(evaluatedBody, StandardCharsets.UTF_8));
        }
        httpUriRequest = put;
        break;
      }
      case "DELETE": {
        httpUriRequest = new HttpDelete(evaluatedUrl);
        break;
      }
      case "HEAD": {
        httpUriRequest = new HttpHead(evaluatedUrl);
        break;
      }
    }

    if (evaluatedHeader != null) {
      for (String header : HEADERS_SPLITTER.split(evaluatedHeader)) {
        List<String> headerPair = HEADER_SPLITTER.splitToList(header);

        if (headerPair.size() == 2) {
          httpUriRequest.addHeader(headerPair.get(0), headerPair.get(1));
        }
      }
    }

    try {
      HttpResponse httpResponse = httpclient.execute(httpUriRequest);
      executionData.setHttpResponseCode(httpResponse.getStatusLine().getStatusCode());
      HttpEntity entity = httpResponse.getEntity();
      executionData.setHttpResponseBody(
          entity != null ? EntityUtils.toString(entity, ContentType.getOrDefault(entity).getCharset()) : "");
    } catch (IOException e) {
      logger.error("Exception: ", e);
      errorMessage = getMessage(e);
      executionData.setHttpResponseCode(500);
      executionData.setHttpResponseBody(getMessage(e));
    }

    boolean status = false;
    try {
      status = (boolean) context.evaluateExpression(assertion, executionData);
      logger.info("assertion status: {}", status);
    } catch (Exception e) {
      errorMessage = getMessage(e);
      logger.error("Error in httpStateAssertion", e);
      status = false;
    }

    ExecutionStatus executionStatus = status ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    ExecutionResponse response = new ExecutionResponse();
    response.setExecutionStatus(executionStatus);

    executionData.setAssertionStatus(executionStatus.name());
    response.setStateExecutionData(executionData);
    response.setErrorMessage(errorMessage);
    return response;
  }

  /**
   * Create activity string.
   *
   * @param executionContext the execution context
   * @return the string
   */
  protected String createActivity(ExecutionContext executionContext) {
    Application app = executionContext.getApp();
    Environment env = executionContext.getEnv();
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

    if (instanceElement != null) {
      activityBuilder.withServiceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .withServiceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .withServiceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .withServiceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .withServiceInstanceId(instanceElement.getUuid())
          .withHostName(instanceElement.getHostElement().getHostName());
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
