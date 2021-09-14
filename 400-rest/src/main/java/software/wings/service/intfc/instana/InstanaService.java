/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.instana;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.instana.InstanaSetupTestNodeData;

public interface InstanaService {
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(InstanaSetupTestNodeData instanaSetupTestNodeData);
}
