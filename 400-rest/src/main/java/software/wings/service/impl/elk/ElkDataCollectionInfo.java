/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.elk;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.ElkConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Elk Data collection Info that is shared for while initiating Data collection task
 * Created by rsingh on 5/18/17.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class ElkDataCollectionInfo extends LogDataCollectionInfo implements ExecutionCapabilityDemander {
  private ElkConfig elkConfig;
  private String indices;
  private String messageField;
  private String timestampField;
  private String timestampFieldFormat;
  private ElkQueryType queryType;

  @Builder
  public ElkDataCollectionInfo(String accountId, String applicationId, String stateExecutionId, String cvConfigId,
      String workflowId, String workflowExecutionId, String serviceId, String query, long startTime, long endTime,
      int startMinute, int collectionTime, String hostnameField, Set<String> hosts,
      List<EncryptedDataDetail> encryptedDataDetails, ElkConfig elkConfig, String indices, String messageField,
      String timestampField, String timestampFieldFormat, ElkQueryType queryType, int initialDelayMinutes) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId, query,
        startTime, endTime, startMinute, collectionTime, hostnameField, hosts, StateType.ELK, encryptedDataDetails,
        initialDelayMinutes);
    this.elkConfig = elkConfig;
    this.indices = indices;
    this.messageField = messageField;
    this.timestampField = timestampField;
    this.timestampFieldFormat = timestampFieldFormat;
    this.queryType = queryType;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return CapabilityHelper.generateDelegateCapabilities(elkConfig, getEncryptedDataDetails(), maskingEvaluator);
  }
}
