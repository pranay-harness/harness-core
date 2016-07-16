/**
 *
 */

package software.wings.api;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.base.MoreObjects;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import software.wings.common.Constants;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.utils.XmlUtils;

import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The Class HttpStateExecutionData.
 *
 * @author Rishi
 */
public class HttpStateExecutionData extends StateExecutionData {
  private String httpUrl;
  private String httpMethod;
  private int httpResponseCode;
  private String httpResponseBody;
  private String assertionStatement;
  private String assertionStatus;

  private Document document;

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
  public String xpath(String path) {
    try {
      return XmlUtils.xpath(document(), path);
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
    putNotNull(executionDetails, "httpUrl", anExecutionDataValue().withValue(httpUrl).withDisplayName("Url").build());
    putNotNull(
        executionDetails, "httpMethod", anExecutionDataValue().withValue(httpMethod).withDisplayName("Method").build());
    putNotNull(executionDetails, "httpResponseCode",
        anExecutionDataValue().withValue(httpResponseCode).withDisplayName("Response Code").build());
    putNotNull(executionDetails, "httpResponseBody",
        anExecutionDataValue()
            .withValue(abbreviate(httpResponseBody, Constants.SUMMARY_PAYLOAD_LIMIT))
            .withDisplayName("Response Body")
            .build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "httpUrl", anExecutionDataValue().withValue(httpUrl).withDisplayName("Url").build());
    putNotNull(
        executionDetails, "httpMethod", anExecutionDataValue().withValue(httpMethod).withDisplayName("Method").build());
    putNotNull(executionDetails, "httpResponseCode",
        anExecutionDataValue().withValue(httpResponseCode).withDisplayName("Response Code").build());
    putNotNull(executionDetails, "httpResponseBody",
        anExecutionDataValue().withValue(httpResponseBody).withDisplayName("Response Body").build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
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
    private String httpUrl;
    private String httpMethod;
    private int httpResponseCode;
    private String httpResponseBody;
    private String assertionStatement;
    private String assertionStatus;

    private Builder() {}

    /**
     * A http state execution data builder.
     *
     * @return the builder
     */
    public static Builder aHttpStateExecutionData() {
      return new Builder();
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With http url builder.
     *
     * @param httpUrl the http url
     * @return the builder
     */
    public Builder withHttpUrl(String httpUrl) {
      this.httpUrl = httpUrl;
      return this;
    }

    /**
     * With http method builder.
     *
     * @param httpMethod the http method
     * @return the builder
     */
    public Builder withHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    /**
     * With http response code builder.
     *
     * @param httpResponseCode the http response code
     * @return the builder
     */
    public Builder withHttpResponseCode(int httpResponseCode) {
      this.httpResponseCode = httpResponseCode;
      return this;
    }

    /**
     * With http response body builder.
     *
     * @param httpResponseBody the http response body
     * @return the builder
     */
    public Builder withHttpResponseBody(String httpResponseBody) {
      this.httpResponseBody = httpResponseBody;
      return this;
    }

    /**
     * With assertion statement builder.
     *
     * @param assertionStatement the assertion statement
     * @return the builder
     */
    public Builder withAssertionStatement(String assertionStatement) {
      this.assertionStatement = assertionStatement;
      return this;
    }

    /**
     * With assertion status builder.
     *
     * @param assertionStatus the assertion status
     * @return the builder
     */
    public Builder withAssertionStatus(String assertionStatus) {
      this.assertionStatus = assertionStatus;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHttpStateExecutionData()
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withHttpUrl(httpUrl)
          .withHttpMethod(httpMethod)
          .withHttpResponseCode(httpResponseCode)
          .withHttpResponseBody(httpResponseBody)
          .withAssertionStatement(assertionStatement)
          .withAssertionStatus(assertionStatus);
    }

    /**
     * Build http state execution data.
     *
     * @return the http state execution data
     */
    public HttpStateExecutionData build() {
      HttpStateExecutionData httpStateExecutionData = new HttpStateExecutionData();
      httpStateExecutionData.setStateName(stateName);
      httpStateExecutionData.setStartTs(startTs);
      httpStateExecutionData.setEndTs(endTs);
      httpStateExecutionData.setStatus(status);
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
