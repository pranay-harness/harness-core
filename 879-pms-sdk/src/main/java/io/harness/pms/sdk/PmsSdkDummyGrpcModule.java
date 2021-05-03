package io.harness.pms.sdk;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc.InterruptProtoServiceBlockingStub;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc.OutcomeProtoServiceBlockingStub;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import io.grpc.inprocess.InProcessChannelBuilder;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkDummyGrpcModule extends AbstractModule {
  private static PmsSdkDummyGrpcModule instance;

  public static PmsSdkDummyGrpcModule getInstance() {
    if (instance == null) {
      instance = new PmsSdkDummyGrpcModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<InterruptProtoServiceBlockingStub>() {
    }).toInstance(InterruptProtoServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(generateUuid()).build()));

    bind(new TypeLiteral<SweepingOutputServiceBlockingStub>() {
    }).toInstance(SweepingOutputServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(generateUuid()).build()));

    bind(new TypeLiteral<OutcomeProtoServiceBlockingStub>() {
    }).toInstance(OutcomeProtoServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(generateUuid()).build()));
  }
}
