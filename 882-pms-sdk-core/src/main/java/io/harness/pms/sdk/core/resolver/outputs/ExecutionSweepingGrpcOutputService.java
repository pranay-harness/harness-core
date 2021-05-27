package io.harness.pms.sdk.core.resolver.outputs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.service.OptionalSweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.grpc.client.PmsSdkGrpcClientUtils;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ExecutionSweepingGrpcOutputService implements ExecutionSweepingOutputService {
  private final SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub;

  @Inject
  public ExecutionSweepingGrpcOutputService(SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub) {
    this.sweepingOutputServiceBlockingStub = sweepingOutputServiceBlockingStub;
  }

  @Override
  public ExecutionSweepingOutput resolve(Ambiance ambiance, RefObject refObject) {
    try {
      SweepingOutputResolveBlobResponse resolve = sweepingOutputServiceBlockingStub.resolve(
          SweepingOutputResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
      return RecastOrchestrationUtils.fromDocumentJson(resolve.getStepTransput(), ExecutionSweepingOutput.class);
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }

  @Override
  public String consumeInternal(Ambiance ambiance, String name, ExecutionSweepingOutput value, int levelsToKeep) {
    return null;
  }

  @Override
  public String consume(Ambiance ambiance, String name, ExecutionSweepingOutput value, String groupName) {
    SweepingOutputConsumeBlobRequest.Builder builder =
        SweepingOutputConsumeBlobRequest.newBuilder().setAmbiance(ambiance).setName(name).setValue(
            RecastOrchestrationUtils.toDocumentJson(value));
    if (EmptyPredicate.isNotEmpty(groupName)) {
      builder.setGroupName(groupName);
    }
    try {
      SweepingOutputConsumeBlobResponse sweepingOutputConsumeBlobResponse =
          sweepingOutputServiceBlockingStub.consume(builder.build());
      return sweepingOutputConsumeBlobResponse.getResponse();
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }

  @Override
  public OptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject) {
    try {
      OptionalSweepingOutputResolveBlobResponse resolve = sweepingOutputServiceBlockingStub.resolveOptional(
          SweepingOutputResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
      return OptionalSweepingOutput.builder()
          .output(RecastOrchestrationUtils.fromDocumentJson(resolve.getStepTransput(), ExecutionSweepingOutput.class))
          .found(resolve.getFound())
          .build();
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }
}
