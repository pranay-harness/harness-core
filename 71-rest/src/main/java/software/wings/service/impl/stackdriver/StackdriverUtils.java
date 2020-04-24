package software.wings.service.impl.stackdriver;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.HttpCapabilityDetailsLevel.QUERY;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.ArrayList;
import java.util.List;

public class StackdriverUtils {
  // https://cloud.google.com/monitoring/api/ref_v3/rest
  private static final String STACKDRIVER_VALIDATION_URL =
      "https://monitoring.googleapis.com/$discovery/rest?version=v1";
  // https://cloud.google.com/logging/docs/reference/v2/rest
  private static final String STACKDRIVER_LOGGING_VALIDATION_URL =
      "https://logging.googleapis.com/$discovery/rest?version=v2";

  private StackdriverUtils() {}

  public static List<ExecutionCapability> fetchRequiredExecutionCapabilitiesForMetrics(
      List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        STACKDRIVER_VALIDATION_URL, QUERY));
    executionCapabilities.addAll(
        CapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(encryptedDataDetails));
    return executionCapabilities;
  }

  public static List<ExecutionCapability> fetchRequiredExecutionCapabilitiesForLogs(
      List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        STACKDRIVER_LOGGING_VALIDATION_URL, QUERY));
    executionCapabilities.addAll(
        CapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(encryptedDataDetails));
    return executionCapabilities;
  }
}
