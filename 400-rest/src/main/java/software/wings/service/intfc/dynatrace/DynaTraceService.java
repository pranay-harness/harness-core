/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.dynatrace;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.dynatrace.DynaTraceApplication;
import software.wings.service.impl.dynatrace.DynaTraceSetupTestNodeData;

import java.util.List;

/**
 * Interface for DynaTrace Service.
 * Created by Pranjal on 09/12/2018
 */
public interface DynaTraceService {
  /**
   * Method to fetch metric data based on Given Service Methods
   * @param setupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(DynaTraceSetupTestNodeData setupTestNodeData);

  List<DynaTraceApplication> getServices(String settingId, boolean shouldResolveAllServices);

  String resolveDynatraceServiceNameToId(String settingId, String serviceName);

  boolean validateDynatraceServiceId(String settingId, String serviceId);
}
