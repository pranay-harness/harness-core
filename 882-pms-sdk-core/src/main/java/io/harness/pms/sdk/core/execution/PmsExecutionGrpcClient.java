package io.harness.pms.sdk.core.execution;

import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc.PmsExecutionServiceBlockingStub;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PmsExecutionGrpcClient {
  private final PmsExecutionServiceBlockingStub pmsExecutionServiceBlockingStub;

  @Inject
  public PmsExecutionGrpcClient(PmsExecutionServiceBlockingStub pmsExecutionServiceBlockingStub) {
    this.pmsExecutionServiceBlockingStub = pmsExecutionServiceBlockingStub;
  }

  public void updateExecutionSummary(ExecutionSummaryUpdateRequest request) {
    pmsExecutionServiceBlockingStub.updateExecutionSummary(request);
  }
}
