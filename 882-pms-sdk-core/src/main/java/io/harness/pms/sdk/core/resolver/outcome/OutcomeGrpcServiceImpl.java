package io.harness.pms.sdk.core.resolver.outcome;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.service.OutcomeConsumeBlobRequest;
import io.harness.pms.contracts.service.OutcomeConsumeBlobResponse;
import io.harness.pms.contracts.service.OutcomeFetchOutcomeBlobRequest;
import io.harness.pms.contracts.service.OutcomeFetchOutcomeBlobResponse;
import io.harness.pms.contracts.service.OutcomeFetchOutcomesBlobRequest;
import io.harness.pms.contracts.service.OutcomeFetchOutcomesBlobResponse;
import io.harness.pms.contracts.service.OutcomeFindAllBlobRequest;
import io.harness.pms.contracts.service.OutcomeFindAllBlobResponse;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc.OutcomeProtoServiceBlockingStub;
import io.harness.pms.contracts.service.OutcomeResolveBlobRequest;
import io.harness.pms.contracts.service.OutcomeResolveBlobResponse;
import io.harness.pms.contracts.service.OutcomeResolveOptionalBlobRequest;
import io.harness.pms.contracts.service.OutcomeResolveOptionalBlobResponse;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.grpc.client.PmsSdkGrpcClientUtils;
import io.harness.pms.sdk.core.resolver.outcome.mapper.PmsOutcomeMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.NonNull;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class OutcomeGrpcServiceImpl implements OutcomeService {
  private final OutcomeProtoServiceBlockingStub outcomeProtoServiceBlockingStub;

  @Inject
  public OutcomeGrpcServiceImpl(OutcomeProtoServiceBlockingStub outcomeProtoServiceBlockingStub) {
    this.outcomeProtoServiceBlockingStub = outcomeProtoServiceBlockingStub;
  }

  @Override
  public List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId) {
    try {
      OutcomeFindAllBlobResponse allByRuntimeId = outcomeProtoServiceBlockingStub.findAllByRuntimeId(
          OutcomeFindAllBlobRequest.newBuilder().setPlanExecutionId(planExecutionId).setRuntimeId(runtimeId).build());
      return PmsOutcomeMapper.convertJsonToOutcome(allByRuntimeId.getOutcomesList());
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }

  @Override
  public List<Outcome> fetchOutcomes(List<String> outcomeInstanceIds) {
    try {
      OutcomeFetchOutcomesBlobResponse outcomeFetchOutcomesBlobResponse = outcomeProtoServiceBlockingStub.fetchOutcomes(
          OutcomeFetchOutcomesBlobRequest.newBuilder().addAllOutcomeInstanceIds(outcomeInstanceIds).build());
      return PmsOutcomeMapper.convertJsonToOutcome(outcomeFetchOutcomesBlobResponse.getOutcomesList());
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }

  @Override
  public Outcome fetchOutcome(@NonNull String outcomeInstanceId) {
    try {
      OutcomeFetchOutcomeBlobResponse outcomeFetchOutcomeBlobResponse = outcomeProtoServiceBlockingStub.fetchOutcome(
          OutcomeFetchOutcomeBlobRequest.newBuilder().setOutcomeInstanceId(outcomeInstanceId).build());
      return PmsOutcomeMapper.convertJsonToOutcome(outcomeFetchOutcomeBlobResponse.getOutcome());
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }

  @Override
  public Outcome resolve(Ambiance ambiance, RefObject refObject) {
    try {
      OutcomeResolveBlobResponse resolve = outcomeProtoServiceBlockingStub.resolve(
          OutcomeResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
      return PmsOutcomeMapper.convertJsonToOutcome(resolve.getStepTransput());
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }

  @Override
  public String consume(Ambiance ambiance, String name, Outcome value, String groupName) {
    try {
      OutcomeConsumeBlobResponse response =
          outcomeProtoServiceBlockingStub.consume(OutcomeConsumeBlobRequest.newBuilder()
                                                      .setAmbiance(ambiance)
                                                      .setName(name)
                                                      .setValue(PmsOutcomeMapper.convertOutcomeValueToJson(value))
                                                      .setGroupName(groupName)
                                                      .build());
      return response.getResponse();
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }

  @Override
  public OptionalOutcome resolveOptional(Ambiance ambiance, RefObject refObject) {
    try {
      OutcomeResolveOptionalBlobResponse response = outcomeProtoServiceBlockingStub.resolveOptional(
          OutcomeResolveOptionalBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
      return OptionalOutcome.builder()
          .found(response.getFound())
          .outcome(PmsOutcomeMapper.convertJsonToOutcome(response.getOutcome()))
          .build();
    } catch (Exception ex) {
      throw PmsSdkGrpcClientUtils.processException(ex);
    }
  }
}
