/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.sumo;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.SumoConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class SumoDataCollectionInfo extends LogDataCollectionInfo implements ExecutionCapabilityDemander {
  private SumoConfig sumoConfig;

  @Builder

  public SumoDataCollectionInfo(String accountId, String applicationId, String stateExecutionId, String cvConfigId,
      String workflowId, String workflowExecutionId, String serviceId, String query, long startTime, long endTime,
      int startMinute, int collectionTime, String hostnameField, Set<String> hosts,
      List<EncryptedDataDetail> encryptedDataDetails, SumoConfig sumoConfig, int initialDelayMinutes) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId, query,
        startTime, endTime, startMinute, collectionTime, hostnameField, hosts, StateType.SUMO, encryptedDataDetails,
        initialDelayMinutes);
    this.sumoConfig = sumoConfig;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return CapabilityHelper.generateDelegateCapabilities(sumoConfig, getEncryptedDataDetails(), maskingEvaluator);
  }
}
