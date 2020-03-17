package io.harness.perpetualtask.internal;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.service.intfc.DelegateService;

import java.util.HashMap;

public class PerpetualTaskRecordHandlerTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String delegateId = "DELEGATE_ID";
  private PerpetualTaskRecord record;
  @Mock private DelegateTask delegateTask;
  private DelegateTaskNotifyResponseData response;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock K8sWatchPerpetualTaskServiceClient k8sWatchPerpetualTaskServiceClient;
  @Mock PerpetualTaskServiceClientRegistry clientRegistry;
  @Mock DelegateService delegateService;
  @Mock PerpetualTaskService perpetualTaskService;
  @InjectMocks PerpetualTaskRecordHandler perpetualTaskRecordHandler;

  @Before
  public void setUp() throws InterruptedException {
    record = PerpetualTaskRecord.builder()
                 .accountId(accountId)
                 .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                 .clientContext(new PerpetualTaskClientContext(new HashMap<>()))
                 .build();
    when(clientRegistry.getClient(isA(PerpetualTaskType.class))).thenReturn(k8sWatchPerpetualTaskServiceClient);
    when(k8sWatchPerpetualTaskServiceClient.getValidationTask(isA(PerpetualTaskClientContext.class), anyString()))
        .thenReturn(delegateTask);
    response = AssignmentTaskResponse.builder()
                   .delegateId(delegateId)
                   .delegateMetaInfo(DelegateMetaInfo.builder().id(delegateId).build())
                   .build();
    when(delegateService.executeTask(isA(DelegateTask.class))).thenReturn(response);
    doNothing().when(perpetualTaskService).setDelegateId(anyString(), eq(delegateId));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testHandle() {
    perpetualTaskRecordHandler.handle(record);
    verify(perpetualTaskService).setDelegateId(anyString(), eq(delegateId));
  }
}
