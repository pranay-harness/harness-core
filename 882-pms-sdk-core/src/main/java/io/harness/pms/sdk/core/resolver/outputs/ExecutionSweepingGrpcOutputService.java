package io.harness.pms.sdk.core.resolver.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class ExecutionSweepingGrpcOutputService implements ExecutionSweepingOutputService {
  private final SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub;

  @Inject
  public ExecutionSweepingGrpcOutputService(SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub) {
    this.sweepingOutputServiceBlockingStub = sweepingOutputServiceBlockingStub;
  }

  @Override
  public SweepingOutput resolve(Ambiance ambiance, RefObject refObject) {
    SweepingOutputResolveBlobResponse resolve = sweepingOutputServiceBlockingStub.resolve(
        SweepingOutputResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
    return DocumentOrchestrationUtils.convertFromDocumentJson(resolve.getStepTransput());
  }

  @Override
  public String consumeInternal(Ambiance ambiance, String name, SweepingOutput value, int levelsToKeep) {
    return null;
  }

  @Override
  public String consume(Ambiance ambiance, String name, SweepingOutput value, String groupName) {
    SweepingOutputConsumeBlobResponse sweepingOutputConsumeBlobResponse =
        sweepingOutputServiceBlockingStub.consume(SweepingOutputConsumeBlobRequest.newBuilder()
                                                      .setAmbiance(ambiance)
                                                      .setName(name)
                                                      .setGroupName(groupName)
                                                      .setValue(DocumentOrchestrationUtils.convertToDocumentJson(value))
                                                      .build());
    return sweepingOutputConsumeBlobResponse.getResponse();
  }
}
