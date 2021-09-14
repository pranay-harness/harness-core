/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.dynatrace;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.DynaTraceConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.dynatrace.DynaTraceApplication;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataRequest;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataResponse;
import software.wings.service.impl.dynatrace.DynaTraceSetupTestNodeData;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 01/29/18.
 */
public interface DynaTraceDelegateService {
  @DelegateTaskType(TaskType.DYNA_TRACE_VALIDATE_CONFIGURATION_TASK)
  boolean validateConfig(@NotNull DynaTraceConfig dynaTraceConfig, List<EncryptedDataDetail> encryptedDataDetails);

  DynaTraceMetricDataResponse fetchMetricData(@NotNull DynaTraceConfig dynaTraceConfig,
      @NotNull DynaTraceMetricDataRequest dataRequest, @NotNull List<EncryptedDataDetail> encryptedDataDetails,
      ThirdPartyApiCallLog apiCallLog);

  @DelegateTaskType(TaskType.DYNA_TRACE_GET_TXNS_WITH_DATA_FOR_NODE)
  List<DynaTraceMetricDataResponse> getMetricsWithDataForNode(DynaTraceConfig value,
      List<EncryptedDataDetail> encryptionDetails, DynaTraceSetupTestNodeData setupTestNodeData,
      ThirdPartyApiCallLog thirdPartyApiCallLog);

  @DelegateTaskType(TaskType.DYNA_TRACE_GET_SERVICES)
  List<DynaTraceApplication> getServices(DynaTraceConfig value, List<EncryptedDataDetail> encryptionDetails,
      ThirdPartyApiCallLog thirdPartyApiCallLog, Boolean shouldResolveAllServices);
}
