package io.harness.pms.sdk.core.resolver.outcome;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.service.*;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc.OutcomeProtoServiceBlockingStub;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.outcome.mapper.PmsOutcomeMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.NonNull;

@OwnedBy(CDC)
@Singleton
public class OutcomeGrpcServiceImpl implements OutcomeService {
  private final OutcomeProtoServiceBlockingStub outcomeProtoServiceBlockingStub;

  @Inject
  public OutcomeGrpcServiceImpl(OutcomeProtoServiceBlockingStub outcomeProtoServiceBlockingStub) {
    this.outcomeProtoServiceBlockingStub = outcomeProtoServiceBlockingStub;
  }

  @Override
  public List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId) {
    OutcomeFindAllBlobResponse allByRuntimeId = outcomeProtoServiceBlockingStub.findAllByRuntimeId(
        OutcomeFindAllBlobRequest.newBuilder().setPlanExecutionId(planExecutionId).setRuntimeId(runtimeId).build());
    return PmsOutcomeMapper.convertJsonToOutcome(allByRuntimeId.getOutcomesList());
  }

  @Override
  public List<Outcome> fetchOutcomes(List<String> outcomeInstanceIds) {
    OutcomeFetchOutcomesBlobResponse outcomeFetchOutcomesBlobResponse = outcomeProtoServiceBlockingStub.fetchOutcomes(
        OutcomeFetchOutcomesBlobRequest.newBuilder().addAllOutcomeInstanceIds(outcomeInstanceIds).build());
    return PmsOutcomeMapper.convertJsonToOutcome(outcomeFetchOutcomesBlobResponse.getOutcomesList());
  }

  @Override
  public Outcome fetchOutcome(@NonNull String outcomeInstanceId) {
    OutcomeFetchOutcomeBlobResponse outcomeFetchOutcomeBlobResponse = outcomeProtoServiceBlockingStub.fetchOutcome(
        OutcomeFetchOutcomeBlobRequest.newBuilder().setOutcomeInstanceId(outcomeInstanceId).build());
    return PmsOutcomeMapper.convertJsonToOutcome(outcomeFetchOutcomeBlobResponse.getOutcome());
  }

  @Override
  public Outcome resolve(Ambiance ambiance, RefObject refObject) {
    OutcomeResolveBlobResponse resolve = outcomeProtoServiceBlockingStub.resolve(
        OutcomeResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
    return PmsOutcomeMapper.convertJsonToOutcome(resolve.getStepTransput());
  }

  @Override
  public String consume(Ambiance ambiance, String name, Outcome value, String groupName) {
    OutcomeConsumeBlobResponse response =
        outcomeProtoServiceBlockingStub.consume(OutcomeConsumeBlobRequest.newBuilder()
                                                    .setAmbiance(ambiance)
                                                    .setName(name)
                                                    .setValue(PmsOutcomeMapper.convertOutcomeValueToJson(value))
                                                    .setGroupName(groupName)
                                                    .build());
    return response.getResponse();
  }
}
