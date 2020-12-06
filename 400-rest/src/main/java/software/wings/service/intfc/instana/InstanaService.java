package software.wings.service.intfc.instana;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.instana.InstanaSetupTestNodeData;

public interface InstanaService {
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(InstanaSetupTestNodeData instanaSetupTestNodeData);
}
