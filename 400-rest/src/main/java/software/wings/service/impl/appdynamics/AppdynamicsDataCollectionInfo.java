/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.appdynamics;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AppDynamicsConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AppdynamicsDataCollectionInfo implements TaskParameters, ExecutionCapabilityDemander {
  private AppDynamicsConfig appDynamicsConfig;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigId;
  private long startTime;
  private long appId;
  private long tierId;
  private int collectionTime;
  private int dataCollectionMinute;
  private Map<String, String> hosts;
  private boolean nodeIdsMapped;
  private List<EncryptedDataDetail> encryptedDataDetails;
  @Default private TimeSeriesMlAnalysisType timeSeriesMlAnalysisType = TimeSeriesMlAnalysisType.COMPARATIVE;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return CapabilityHelper.generateDelegateCapabilities(appDynamicsConfig, encryptedDataDetails, maskingEvaluator);
  }
}
