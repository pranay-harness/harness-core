package software.wings.sm.states;

import static com.google.common.base.Ascii.toUpperCase;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import com.google.common.base.Splitter;

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
public class HttpState extends State {
  private static final Splitter HEADERS_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

  private static final Splitter HEADER_SPLITTER = Splitter.on(":").trimResults();
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private String url;
  private String method;
  private String header;
  private String body;
  private String assertion;
  private int socketTimeoutMillis = 10000;

  /**
   * Create a new Http State with given name.
   *
   * @param name name of the state.
   */
  public HttpState(String name) {
    super(name, StateType.HTTP.name());
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
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

    for (String header : HEADERS_SPLITTER.split(evaluatedHeader)) {
      List<String> headerPair = HEADER_SPLITTER.splitToList(header);

      if (headerPair.size() == 2) {
        request.addHeader(headerPair.get(0), headerPair.get(1));
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

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public String getAssertion() {
    return assertion;
  }

  public void setAssertion(String assertion) {
    this.assertion = assertion;
  }

  public int getSocketTimeoutMillis() {
    return socketTimeoutMillis;
  }

  public void setSocketTimeoutMillis(int socketTimeoutMillis) {
    this.socketTimeoutMillis = socketTimeoutMillis;
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#toString()
   */
  @Override
  public String toString() {
    return "HttpState [url=" + url + ", method=" + method + ", header=" + header + ", body=" + body
        + ", assertion=" + assertion + "]";
  }

  public static final class Builder {
    private String name;
    private String url;
    private String method;
    private String header;
    private String body;
    private String assertion;
    private int socketTimeoutMillis = 10000;

    private Builder() {}

    public static Builder aHttpState() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder withMethod(String method) {
      this.method = method;
      return this;
    }

    public Builder withHeader(String header) {
      this.header = header;
      return this;
    }

    public Builder withBody(String body) {
      this.body = body;
      return this;
    }

    public Builder withAssertion(String assertion) {
      this.assertion = assertion;
      return this;
    }

    public Builder withSocketTimeoutMillis(int socketTimeoutMillis) {
      this.socketTimeoutMillis = socketTimeoutMillis;
      return this;
    }

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
