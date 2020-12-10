package io.harness.pms.sdk.core.resolver.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.pms.service.*;
import io.harness.pms.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;

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
    return JsonOrchestrationUtils.asObject(resolve.getStepTransput(), SweepingOutput.class);
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
                                                      .setValue(value.toJson())
                                                      .build());
    return sweepingOutputConsumeBlobResponse.getResponse();
  }
}
