package software.wings.sm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import software.wings.app.WingsBootstrap;
import software.wings.utils.XmlUtils;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

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
   * @param name
   *          name of the state.
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

  public static class HttpStateExecutionData extends StateExecutionData {
    private static final long serialVersionUID = -435324810208952473L;
    private String httpUrl;
    private String httpMethod;
    private int httpResponseCode;
    private String httpResponseBody;
    private String assertionStatement;
    private String assertionStatus;

    private transient Document document;
    private transient XmlUtils xmlUtils;

    public String getHttpUrl() {
      return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
      this.httpUrl = httpUrl;
    }

    public String getHttpMethod() {
      return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
    }

    public int getHttpResponseCode() {
      return httpResponseCode;
    }

    public void setHttpResponseCode(int httpResponseCode) {
      this.httpResponseCode = httpResponseCode;
    }

    public String getHttpResponseBody() {
      return httpResponseBody;
    }

    public void setHttpResponseBody(String httpResponseBody) {
      this.httpResponseBody = httpResponseBody;
    }

    public String getAssertionStatement() {
      return assertionStatement;
    }

    public void setAssertionStatement(String assertionStatement) {
      this.assertionStatement = assertionStatement;
    }

    public String getAssertionStatus() {
      return assertionStatus;
    }

    public void setAssertionStatus(String assertionStatus) {
      this.assertionStatus = assertionStatus;
    }

    public boolean xmlFormat() {
      try {
        getDocument();
        return true;
      } catch (Exception e) {
        return false;
      }
    }
    public String xpath(String path) {
      try {
        return getXmlUtils().xpath(getDocument(), path);
      } catch (Exception e) {
        return null;
      }
    }

    private Document getDocument() throws ParserConfigurationException, SAXException, IOException {
      if (document == null) {
        document = getXmlUtils().parse(httpResponseBody);
      }
      return document;
    }
    private XmlUtils getXmlUtils() {
      return WingsBootstrap.lookup(XmlUtils.class);
    }
  }
}
