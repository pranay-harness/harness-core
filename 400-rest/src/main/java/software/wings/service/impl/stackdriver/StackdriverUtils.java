/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.stackdriver;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.HttpCapabilityDetailsLevel.QUERY;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

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
      List<EncryptedDataDetail> encryptedDataDetails, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        STACKDRIVER_VALIDATION_URL, QUERY, maskingEvaluator));
    executionCapabilities.addAll(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
        encryptedDataDetails, maskingEvaluator));
    return executionCapabilities;
  }

  public static List<ExecutionCapability> fetchRequiredExecutionCapabilitiesForLogs(
      List<EncryptedDataDetail> encryptedDataDetails, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        STACKDRIVER_LOGGING_VALIDATION_URL, QUERY, maskingEvaluator));
    executionCapabilities.addAll(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
        encryptedDataDetails, maskingEvaluator));
    return executionCapabilities;
  }
}
