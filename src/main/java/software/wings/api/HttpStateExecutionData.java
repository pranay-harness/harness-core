/**
 *
 */
package software.wings.api;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import software.wings.app.WingsBootstrap;
import software.wings.sm.StateExecutionData;
import software.wings.utils.XmlUtils;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

/**
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
