package io.harness.pms.sdk.core.resolver.outputs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.service.OptionalSweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputListRequest;
import io.harness.pms.contracts.service.SweepingOutputListResponse;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.PmsSdkGrpcNamesUtil;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.utils.PmsGrpcClientUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ExecutionSweepingGrpcOutputService implements ExecutionSweepingOutputService {
  private final Injector injector;

  @Inject
  public ExecutionSweepingGrpcOutputService(Injector injector) {
    this.injector = injector;
  }

  @Override
  public ExecutionSweepingOutput resolve(Ambiance ambiance, RefObject refObject) {
    SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub =
        injector.getInstance(Key.get(SweepingOutputServiceBlockingStub.class,
            Names.named(PmsSdkGrpcNamesUtil.getPmsClientStubAnnotation(
                AmbianceUtils.obtainCurrentLevel(ambiance).getServiceName()))));
    SweepingOutputResolveBlobResponse resolve =
        PmsGrpcClientUtils.retryAndProcessException(sweepingOutputServiceBlockingStub::resolve,
            SweepingOutputResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
    return RecastOrchestrationUtils.fromJson(resolve.getStepTransput(), ExecutionSweepingOutput.class);
  }

  @Override
  public String consume(Ambiance ambiance, String name, ExecutionSweepingOutput value, String groupName) {
    SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub =
        injector.getInstance(Key.get(SweepingOutputServiceBlockingStub.class,
            Names.named(PmsSdkGrpcNamesUtil.getPmsClientStubAnnotation(
                AmbianceUtils.obtainCurrentLevel(ambiance).getServiceName()))));

    SweepingOutputConsumeBlobRequest.Builder builder =
        SweepingOutputConsumeBlobRequest.newBuilder().setAmbiance(ambiance).setName(name).setValue(
            RecastOrchestrationUtils.toJson(value));
    if (EmptyPredicate.isNotEmpty(groupName)) {
      builder.setGroupName(groupName);
    }

    SweepingOutputConsumeBlobResponse sweepingOutputConsumeBlobResponse =
        PmsGrpcClientUtils.retryAndProcessException(sweepingOutputServiceBlockingStub::consume, builder.build());
    return sweepingOutputConsumeBlobResponse.getResponse();
  }

  @Override
  public OptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject) {
    SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub =
        injector.getInstance(Key.get(SweepingOutputServiceBlockingStub.class,
            Names.named(PmsSdkGrpcNamesUtil.getPmsClientStubAnnotation(
                AmbianceUtils.obtainCurrentLevel(ambiance).getServiceName()))));

    OptionalSweepingOutputResolveBlobResponse resolve =
        PmsGrpcClientUtils.retryAndProcessException(sweepingOutputServiceBlockingStub::resolveOptional,
            SweepingOutputResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
    return OptionalSweepingOutput.builder()
        .output(RecastOrchestrationUtils.fromJson(resolve.getStepTransput(), ExecutionSweepingOutput.class))
        .found(resolve.getFound())
        .build();
  }

  @Override
  public List<OptionalSweepingOutput> listOutputsWithGivenNameAndSetupIds(
      Ambiance ambiance, String name, List<String> nodeIds) {
    SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub =
        injector.getInstance(Key.get(SweepingOutputServiceBlockingStub.class,
            Names.named(PmsSdkGrpcNamesUtil.getPmsClientStubAnnotation(
                AmbianceUtils.obtainCurrentLevel(ambiance).getServiceName()))));
    SweepingOutputListResponse resolve =
        PmsGrpcClientUtils.retryAndProcessException(sweepingOutputServiceBlockingStub::listOutputsUsingNodeIds,
            SweepingOutputListRequest.newBuilder().setAmbiance(ambiance).setName(name).addAllNodeIds(nodeIds).build());
    List<OptionalSweepingOutput> optionalSweepingOutputs = new ArrayList<>();
    for (OptionalSweepingOutputResolveBlobResponse rawOptionalSweepingOutput :
        resolve.getSweepingOutputResolveBlobResponsesList()) {
      optionalSweepingOutputs.add(OptionalSweepingOutput.builder()
                                      .output(RecastOrchestrationUtils.fromJson(
                                          rawOptionalSweepingOutput.getStepTransput(), ExecutionSweepingOutput.class))
                                      .found(rawOptionalSweepingOutput.getFound())
                                      .build());
    }
    return optionalSweepingOutputs;
  }
}
