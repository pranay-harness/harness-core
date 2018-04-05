/**
 *
 */

package software.wings.api;

import com.google.common.base.MoreObjects;

import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import software.wings.common.Constants;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.utils.JsonUtils;
import software.wings.utils.XmlUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The Class HttpStateExecutionData.
 *
 * @author Rishi
 */
public class HttpStateExecutionData extends StateExecutionData implements NotifyResponseData {
  private String httpUrl;
  private String httpMethod;
  private int httpResponseCode;
  private String httpResponseBody;
  private String assertionStatement;
  private String assertionStatus;

  @Transient private transient Document document;

  public HttpStateExecutionData() {}

  /**
   * Gets http url.
   *
   * @return the http url
   */
  public String getHttpUrl() {
    return httpUrl;
  }

  /**
   * Sets http url.
   *
   * @param httpUrl the http url
   */
  public void setHttpUrl(String httpUrl) {
    this.httpUrl = httpUrl;
  }

  /**
   * Gets http method.
   *
   * @return the http method
   */
  public String getHttpMethod() {
    return httpMethod;
  }

  /**
   * Sets http method.
   *
   * @param httpMethod the http method
   */
  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  /**
   * Gets http response code.
   *
   * @return the http response code
   */
  public int getHttpResponseCode() {
    return httpResponseCode;
  }

  /**
   * Sets http response code.
   *
   * @param httpResponseCode the http response code
   */
  public void setHttpResponseCode(int httpResponseCode) {
    this.httpResponseCode = httpResponseCode;
  }

  /**
   * Gets http response body.
   *
   * @return the http response body
   */
  public String getHttpResponseBody() {
    return httpResponseBody;
  }

  /**
   * Sets http response body.
   *
   * @param httpResponseBody the http response body
   */
  public void setHttpResponseBody(String httpResponseBody) {
    this.httpResponseBody = httpResponseBody;
  }

  /**
   * Gets assertion statement.
   *
   * @return the assertion statement
   */
  public String getAssertionStatement() {
    return assertionStatement;
  }

  /**
   * Sets assertion statement.
   *
   * @param assertionStatement the assertion statement
   */
  public void setAssertionStatement(String assertionStatement) {
    this.assertionStatement = assertionStatement;
  }

  /**
   * Gets assertion status.
   *
   * @return the assertion status
   */
  public String getAssertionStatus() {
    return assertionStatus;
  }

  /**
   * Sets assertion status.
   *
   * @param assertionStatus the assertion status
   */
  public void setAssertionStatus(String assertionStatus) {
    this.assertionStatus = assertionStatus;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("httpUrl", httpUrl)
        .add("httpMethod", httpMethod)
        .add("httpResponseCode", httpResponseCode)
        .add("httpResponseBody", httpResponseBody)
        .add("assertionStatement", assertionStatement)
        .add("assertionStatus", assertionStatus)
        .add("document", document)
        .toString();
  }

  /**
   * Xml format.
   *
   * @return true, if successful
   */
  public boolean xmlFormat() {
    try {
      document();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Xpath.
   *
   * @param path the path
   * @return the string
   */
  public Object xpath(String path) {
    try {
      return XmlUtils.xpath(document(), path);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * json path.
   *
   * @param path the path
   * @return the string
   */
  public Object jsonpath(String path) {
    try {
      return JsonUtils.jsonPath(httpResponseBody, path);
    } catch (Exception e) {
      return null;
    }
  }

  private Document document() throws ParserConfigurationException, SAXException, IOException {
    if (document == null) {
      document = XmlUtils.parse(httpResponseBody);
    }
    return document;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "httpUrl", ExecutionDataValue.builder().displayName("Url").value(httpUrl).build());
    putNotNull(
        executionDetails, "httpMethod", ExecutionDataValue.builder().displayName("Method").value(httpMethod).build());
    putNotNull(executionDetails, "httpResponseCode",
        ExecutionDataValue.builder().displayName("Response Code").value(httpResponseCode).build());
    putNotNull(executionDetails, "httpResponseBody",
        ExecutionDataValue.builder()
            .displayName("Response Body")
            .value(StringUtils.abbreviate(httpResponseBody, Constants.SUMMARY_PAYLOAD_LIMIT))
            .build());
    putNotNull(executionDetails, "assertionStatement",
        ExecutionDataValue.builder().displayName("Assertion").value(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        ExecutionDataValue.builder().displayName("Assertion Result").value(assertionStatus).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "httpUrl", ExecutionDataValue.builder().displayName("Url").value(httpUrl).build());
    putNotNull(
        executionDetails, "httpMethod", ExecutionDataValue.builder().displayName("Method").value(httpMethod).build());
    putNotNull(executionDetails, "httpResponseCode",
        ExecutionDataValue.builder().displayName("Response Code").value(httpResponseCode).build());
    putNotNull(executionDetails, "httpResponseBody",
        ExecutionDataValue.builder().displayName("Response Body").value(httpResponseBody).build());
    putNotNull(executionDetails, "assertionStatement",
        ExecutionDataValue.builder().displayName("Assertion").value(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        ExecutionDataValue.builder().displayName("Assertion Result").value(assertionStatus).build());
    return executionDetails;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;
    private String httpUrl;
    private String httpMethod;
    private int httpResponseCode;
    private String httpResponseBody;
    private String assertionStatement;
    private String assertionStatus;

    private Builder() {}

    public static Builder aHttpStateExecutionData() {
      return new Builder();
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public Builder withHttpUrl(String httpUrl) {
      this.httpUrl = httpUrl;
      return this;
    }

    public Builder withHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    public Builder withHttpResponseCode(int httpResponseCode) {
      this.httpResponseCode = httpResponseCode;
      return this;
    }

    public Builder withHttpResponseBody(String httpResponseBody) {
      this.httpResponseBody = httpResponseBody;
      return this;
    }

    public Builder withAssertionStatement(String assertionStatement) {
      this.assertionStatement = assertionStatement;
      return this;
    }

    public Builder withAssertionStatus(String assertionStatus) {
      this.assertionStatus = assertionStatus;
      return this;
    }

    public Builder but() {
      return aHttpStateExecutionData()
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg)
          .withHttpUrl(httpUrl)
          .withHttpMethod(httpMethod)
          .withHttpResponseCode(httpResponseCode)
          .withHttpResponseBody(httpResponseBody)
          .withAssertionStatement(assertionStatement)
          .withAssertionStatus(assertionStatus);
    }

    public HttpStateExecutionData build() {
      HttpStateExecutionData httpStateExecutionData = new HttpStateExecutionData();
      httpStateExecutionData.setStateName(stateName);
      httpStateExecutionData.setStartTs(startTs);
      httpStateExecutionData.setEndTs(endTs);
      httpStateExecutionData.setStatus(status);
      httpStateExecutionData.setErrorMsg(errorMsg);
      httpStateExecutionData.setHttpUrl(httpUrl);
      httpStateExecutionData.setHttpMethod(httpMethod);
      httpStateExecutionData.setHttpResponseCode(httpResponseCode);
      httpStateExecutionData.setHttpResponseBody(httpResponseBody);
      httpStateExecutionData.setAssertionStatement(assertionStatement);
      httpStateExecutionData.setAssertionStatus(assertionStatus);
      return httpStateExecutionData;
    }
  }
}
