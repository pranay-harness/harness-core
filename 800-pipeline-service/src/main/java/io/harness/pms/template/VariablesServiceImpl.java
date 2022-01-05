package io.harness.pms.template;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.service.VariableMergeResponseProto;
import io.harness.pms.contracts.service.VariablesServiceGrpc.VariablesServiceImplBase;
import io.harness.pms.contracts.service.VariablesServiceRequest;
import io.harness.pms.mappers.VariablesResponseDtoMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.variables.VariableMergeServiceResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
@Singleton
public class VariablesServiceImpl extends VariablesServiceImplBase {
  private final PMSPipelineService pmsPipelineService;

  @Inject
  public VariablesServiceImpl(PMSPipelineService pmsPipelineService) {
    this.pmsPipelineService = pmsPipelineService;
  }

  @Override
  public void getVariables(
      VariablesServiceRequest request, StreamObserver<VariableMergeResponseProto> responseObserver) {
    VariableMergeServiceResponse variablesResponse = pmsPipelineService.createVariablesResponse(request.getYaml());
    VariableMergeResponseProto variableMergeResponseProto = VariablesResponseDtoMapper.toProto(variablesResponse);
    responseObserver.onNext(variableMergeResponseProto);
    responseObserver.onCompleted();
  }
}
