/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
