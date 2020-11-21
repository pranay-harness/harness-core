package software.wings.service.impl.analysis;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.HttpCapabilityDetailsLevel.QUERY;

import static software.wings.common.VerificationConstants.DELAY_MINUTES;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.sm.StateType;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;
import software.wings.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
  private String hostnameSeparator;

  @Default private boolean shouldDoHostBasedFiltering = true;
  private boolean fixedHostName;
  // initial delay in LogDataCollectionInfo is not used because we want this value to be default to DELAY_MINUTE instead
  // of 0 Also this is done to reduce risk refactoring and touching more code. Not adding it to builder because this is
  // used by only data dog log (For per minute task)
  @Deprecated private int delayMinutes = DELAY_MINUTES;

  @Builder
  public CustomLogDataCollectionInfo(String baseUrl, String validationUrl, String dataUrl,
      Map<String, Map<String, ResponseMapper>> responseDefinition, Map<String, String> headers,
      Map<String, String> options, Map<String, Object> body, int collectionFrequency, String accountId,
      String applicationId, String stateExecutionId, String cvConfidId, String workflowId, String workflowExecutionId,
      String serviceId, String query, long startTime, long endTime, int startMinute, int collectionTime,
      String hostnameField, Set<String> hosts, StateType stateType, List<EncryptedDataDetail> encryptedDataDetails,
      int initialDelayMinutes, String hostnameSeparator, boolean shouldDoHostBasedFiltering, boolean fixedHostName) {
    super(accountId, applicationId, stateExecutionId, cvConfidId, workflowId, workflowExecutionId, serviceId, query,
        startTime, endTime, startMinute, collectionTime, hostnameField, hosts, stateType, encryptedDataDetails,
        initialDelayMinutes);
    this.baseUrl = baseUrl;
    this.validationUrl = validationUrl;
    this.dataUrl = dataUrl;
    this.logResponseDefinition = responseDefinition;
    this.headers = headers;
    this.options = options;
    this.body = body;
    this.collectionFrequency = collectionFrequency;
    this.hostnameSeparator = hostnameSeparator;
    this.shouldDoHostBasedFiltering = shouldDoHostBasedFiltering;
    this.fixedHostName = fixedHostName;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        Utils.appendPathToBaseUrl(getBaseUrl(), getValidationUrl()), QUERY));
    executionCapabilities.addAll(
        CapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(encryptedDataDetails));
    return executionCapabilities;
  }
}
