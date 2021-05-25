package io.harness.pms.sdk.core.interrupt;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc.InterruptProtoServiceBlockingStub;
import io.harness.pms.contracts.service.InterruptRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class PMSInterruptServiceGrpcImpl implements PMSInterruptService {
  @Inject private InterruptProtoServiceBlockingStub interruptProtoServiceBlockingStub;

  @Override
  public void handleAbort(String notifyId) {
    interruptProtoServiceBlockingStub.handleAbort(InterruptRequest.newBuilder().setNotifyId(notifyId).build());
  }

  @Override
  public void handleFailure(String notifyId) {
    interruptProtoServiceBlockingStub.handleFailure(InterruptRequest.newBuilder().setNotifyId(notifyId).build());
  }
}
