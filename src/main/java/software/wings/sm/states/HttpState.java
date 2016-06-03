package software.wings.sm.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HttpStateExecutionData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;

// TODO: Auto-generated Javadoc

/**
 * Http state which makes a call to http service.
 *
 * @author Rishi
 */
public class HttpState extends State {
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private String url;
  private String method;
  private String header;
  private String body;
  private String assertion;

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
      evaluatedBody = context.renderExpression(body);
      logger.info("evaluatedBody: {}", evaluatedBody);
    }

    String evaluatedHeader = header;
    if (evaluatedHeader != null) {
      evaluatedHeader = context.renderExpression(body);
      logger.info("evaluatedHeader: {}", evaluatedHeader);
    }

    // TODO - http call

    HttpStateExecutionData executionData = new HttpStateExecutionData();
    executionData.setHttpUrl(evaluatedUrl);
    executionData.setHttpMethod(method);
    executionData.setHttpResponseCode(200);
    executionData.setHttpResponseBody("<response><abc></abc><health><status>Enabled</status></health></response>");
    executionData.setAssertionStatement(assertion);

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

  /* (non-Javadoc)
   * @see software.wings.sm.State#toString()
   */
  @Override
  public String toString() {
    return "HttpState [url=" + url + ", method=" + method + ", header=" + header + ", body=" + body
        + ", assertion=" + assertion + "]";
  }
}
