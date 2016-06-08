/**
 *
 */

package software.wings.api;

import com.google.common.base.MoreObjects;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.utils.XmlUtils;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

// TODO: Auto-generated Javadoc

/**
 * The Class HttpStateExecutionData.
 *
 * @author Rishi
 */
public class HttpStateExecutionData extends StateExecutionData {
  private static final long serialVersionUID = -435324810208952473L;
  private String httpUrl;
  private String httpMethod;
  private int httpResponseCode;
  private String httpResponseBody;
  private String assertionStatement;
  private String assertionStatus;

  private transient Document document;

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
