package io.harness.perpetualtask.internal;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.Delegate;
import software.wings.service.impl.DelegateDao;
import software.wings.service.intfc.DelegateService;

import java.util.HashMap;

public class DisconnectedDelegateHandlerTest extends CategoryTest {
  private String taskId = "TASK_ID";
  private String accountId = "ACCOUNT_ID";
  private PerpetualTaskRecord record;
  private String delegateId = "DELEGATE_ID";
  private Delegate delegate;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock PerpetualTaskService perpetualTaskService;
  @Mock DelegateService delegateService;
  @Mock DelegateDao delegateDao;
  @InjectMocks DisconnectedDelegateHandler handler;

  @Before
  public void setUp() {
    delegate = Delegate.builder().accountId(accountId).uuid(delegateId).build();
    record = PerpetualTaskRecord.builder()
                 .uuid(taskId)
                 .accountId(accountId)
                 .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                 .clientContext(new PerpetualTaskClientContext(new HashMap<>()))
                 .delegateId(delegateId)
                 .build();
    when(delegateDao.get(eq(delegateId))).thenReturn(delegate);
    when(delegateService.checkDelegateConnected(eq(delegateId))).thenReturn(false);
    when(perpetualTaskService.resetTask(eq(accountId), eq(delegateId))).thenReturn(true);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldResetWhenDelegateDisconnected() {
    handler.handle(record);
    verify(perpetualTaskService).resetTask(eq(accountId), eq(taskId));
  }
}
