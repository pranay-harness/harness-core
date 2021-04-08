package io.harness.pms.sdk;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceBlockingStub;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc.InterruptProtoServiceBlockingStub;

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
    bind(new TypeLiteral<NodeExecutionProtoServiceBlockingStub>() {})
        .toInstance(
            NodeExecutionProtoServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(generateUuid()).build()));

    bind(new TypeLiteral<InterruptProtoServiceBlockingStub>() {
    }).toInstance(InterruptProtoServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(generateUuid()).build()));
  }
}
