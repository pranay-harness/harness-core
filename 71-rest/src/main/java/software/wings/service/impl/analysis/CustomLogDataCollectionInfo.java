package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.StateType;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class CustomLogDataCollectionInfo extends LogDataCollectionInfo {
  private String baseUrl;
  private String validationUrl;
  private String dataUrl;
  private Map<String, Map<String, ResponseMapper>> logResponseDefinition;
  private Map<String, String> headers;
  private Map<String, String> options;
  private Map<String, Object> body;
  private int collectionFrequency;
  private boolean shouldInspectHosts;
  private String hostnameSeparator;

  @Builder
  public CustomLogDataCollectionInfo(String baseUrl, String validationUrl, String dataUrl,
      Map<String, Map<String, ResponseMapper>> responseDefinition, Map<String, String> headers,
      Map<String, String> options, Map<String, Object> body, int collectionFrequency, boolean shouldInspectHosts,
      String accountId, String applicationId, String stateExecutionId, String cvConfidId, String workflowId,
      String workflowExecutionId, String serviceId, String query, long startTime, long endTime, int startMinute,
      int collectionTime, String hostnameField, Set<String> hosts, StateType stateType,
      List<EncryptedDataDetail> encryptedDataDetails, String hostnameSeparator) {
    super(accountId, applicationId, stateExecutionId, cvConfidId, workflowId, workflowExecutionId, serviceId, query,
        startTime, endTime, startMinute, collectionTime, hostnameField, hosts, stateType, encryptedDataDetails);
    this.baseUrl = baseUrl;
    this.validationUrl = validationUrl;
    this.dataUrl = dataUrl;
    this.logResponseDefinition = responseDefinition;
    this.headers = headers;
    this.options = options;
    this.body = body;
    this.collectionFrequency = collectionFrequency;
    this.shouldInspectHosts = shouldInspectHosts;
    this.hostnameSeparator = hostnameSeparator;
  }
}
