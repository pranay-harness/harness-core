package software.wings.api;

import io.harness.serializer.XmlUtils;
import io.harness.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.w3c.dom.Document;
import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The Class HttpStateExecutionData.
 *
 * @author Rishi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class HttpStateExecutionData extends StateExecutionData implements ResponseData {
  private String httpUrl;
  private String httpMethod;
  private int httpResponseCode;
  private String httpResponseBody;
  private String assertionStatement;
  private String assertionStatus;

  @Transient private transient Document document;

  @Builder
  public HttpStateExecutionData(String stateName, String stateType, Long startTs, Long endTs, ExecutionStatus status,
      String errorMsg, Integer waitInterval, ContextElement element, Map<String, Object> stateParams,
      Map<String, Object> variables, String httpUrl, String httpMethod, int httpResponseCode, String httpResponseBody,
      String assertionStatement, String assertionStatus, Document document) {
    super(stateName, stateType, startTs, endTs, status, errorMsg, waitInterval, element, stateParams, variables);
    this.httpUrl = httpUrl;
    this.httpMethod = httpMethod;
    this.httpResponseCode = httpResponseCode;
    this.httpResponseBody = httpResponseBody;
    this.assertionStatement = assertionStatement;
    this.assertionStatus = assertionStatus;
    this.document = document;
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

  private Document document() throws ParserConfigurationException, IOException, org.xml.sax.SAXException {
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
}
