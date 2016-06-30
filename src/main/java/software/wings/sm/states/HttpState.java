package software.wings.sm.states;

import static com.google.common.base.Ascii.toUpperCase;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HttpStateExecutionData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;

// TODO: Auto-generated Javadoc

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

    Request request = null;
    switch (toUpperCase(method)) {
      case "GET": {
        request = Request.Get(evaluatedUrl);
        break;
      }
      case "POST": {
        request = Request.Post(evaluatedUrl);
        if (evaluatedBody != null) {
          request.bodyByteArray(evaluatedBody.getBytes(UTF_8));
        }
        break;
      }
      case "PUT": {
        request = Request.Put(evaluatedUrl);
        if (evaluatedBody != null) {
          request.bodyByteArray(evaluatedBody.getBytes(UTF_8));
        }
        break;
      }
      case "DELETE": {
        request = Request.Delete(evaluatedUrl);
        break;
      }
      case "HEAD": {
        request = Request.Head(evaluatedUrl);
        break;
      }
    }

    if (evaluatedHeader != null) {
      for (String header : HEADERS_SPLITTER.split(evaluatedHeader)) {
        List<String> headerPair = HEADER_SPLITTER.splitToList(header);

        if (headerPair.size() == 2) {
          request.addHeader(headerPair.get(0), headerPair.get(1));
        }
      }
    }

    try {
      HttpResponse httpResponse =
          request.connectTimeout(2000).socketTimeout(socketTimeoutMillis).execute().returnResponse();
      executionData.setHttpResponseCode(httpResponse.getStatusLine().getStatusCode());
      HttpEntity entity = httpResponse.getEntity();
      executionData.setHttpResponseBody(
          entity != null ? EntityUtils.toString(entity, ContentType.getOrDefault(entity).getCharset()) : "");
    } catch (IOException e) {
      executionData.setHttpResponseCode(500);
      executionData.setHttpResponseBody(getMessage(e));
    }

    boolean status = false;
    try {
      status = (boolean) context.evaluateExpression(assertion, executionData);
      logger.info("assertion status: {}", status);
    } catch (Exception e) {
      logger.error("Error in httpStateAssertion", e);
      status = false;
    }

    ExecutionStatus executionStatus = status ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    ExecutionResponse response = new ExecutionResponse();
    response.setExecutionStatus(executionStatus);

    executionData.setAssertionStatus(executionStatus.name());
    response.setStateExecutionData(executionData);
    return response;
  }

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
