package io.harness.pms.plan.execution.data.service.outcome;

import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.pms.contracts.service.*;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc.OutcomeProtoServiceImplBase;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import java.util.List;

// TODO (archit) : Right now this is acting just as a wrapper for #PmsSweepingOutputService. It also mark the module
// for understanding. Merge the two later
public class OutcomeServiceGrpcServerImpl extends OutcomeProtoServiceImplBase {
  private final PmsOutcomeService pmsOutcomeService;

  @Inject
  public OutcomeServiceGrpcServerImpl(PmsOutcomeService pmsOutcomeService) {
    this.pmsOutcomeService = pmsOutcomeService;
  }

  @Override
  public void resolve(OutcomeResolveBlobRequest request, StreamObserver<OutcomeResolveBlobResponse> responseObserver) {
    String resolve = pmsOutcomeService.resolve(request.getAmbiance(), request.getRefObject());
    responseObserver.onNext(OutcomeResolveBlobResponse.newBuilder().setStepTransput(resolve).build());
    responseObserver.onCompleted();
  }

  @Override
  public void consume(OutcomeConsumeBlobRequest request, StreamObserver<OutcomeConsumeBlobResponse> responseObserver) {
    String consume =
        pmsOutcomeService.consume(request.getAmbiance(), request.getName(), request.getValue(), request.getGroupName());
    responseObserver.onNext(OutcomeConsumeBlobResponse.newBuilder().setResponse(consume).build());
    responseObserver.onCompleted();
  }

  @Override
  public void findAllByRuntimeId(
      OutcomeFindAllBlobRequest request, StreamObserver<OutcomeFindAllBlobResponse> responseObserver) {
    List<String> allByRuntimeId =
        pmsOutcomeService.findAllByRuntimeId(request.getPlanExecutionId(), request.getRuntimeId());
    responseObserver.onNext(OutcomeFindAllBlobResponse.newBuilder().addAllOutcomes(allByRuntimeId).build());
    responseObserver.onCompleted();
  }

  @Override
  public void fetchOutcomes(
      OutcomeFetchOutcomesBlobRequest request, StreamObserver<OutcomeFetchOutcomesBlobResponse> responseObserver) {
    List<String> outcomes = pmsOutcomeService.fetchOutcomes(request.getOutcomeInstanceIdsList());
    responseObserver.onNext(OutcomeFetchOutcomesBlobResponse.newBuilder().addAllOutcomes(outcomes).build());
    responseObserver.onCompleted();
  }

  @Override
  public void fetchOutcome(
      OutcomeFetchOutcomeBlobRequest request, StreamObserver<OutcomeFetchOutcomeBlobResponse> responseObserver) {
    String outcomeJson = pmsOutcomeService.fetchOutcome(request.getOutcomeInstanceId());
    responseObserver.onNext(OutcomeFetchOutcomeBlobResponse.newBuilder().setOutcome(outcomeJson).build());
    responseObserver.onCompleted();
  }
}
