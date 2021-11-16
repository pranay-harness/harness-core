package io.harness.pms.sdk.core.interrupt;

import static io.harness.rule.OwnerRule.SAHIL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc.InterruptProtoServiceBlockingStub;
import io.harness.pms.contracts.service.InterruptRequest;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(HarnessTeam.PIPELINE)
@RunWith(PowerMockRunner.class)
@PrepareForTest(InterruptProtoServiceBlockingStub.class)
public class PMSInterruptServiceGrpcImplTest extends PmsSdkCoreTestBase {
  private static String NOTIFY_ID = "notifyId";
  @Mock InterruptProtoServiceBlockingStub interruptProtoServiceBlockingStub;
  @InjectMocks PMSInterruptServiceGrpcImpl pmsInterruptServiceGrpc;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleAbort() {
    pmsInterruptServiceGrpc.handleAbort(NOTIFY_ID);
    Mockito.verify(interruptProtoServiceBlockingStub)
        .handleAbort(InterruptRequest.newBuilder().setNotifyId(NOTIFY_ID).build());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleFailure() {
    pmsInterruptServiceGrpc.handleFailure(NOTIFY_ID);
    Mockito.verify(interruptProtoServiceBlockingStub)
        .handleFailure(InterruptRequest.newBuilder().setNotifyId(NOTIFY_ID).build());
  }
}