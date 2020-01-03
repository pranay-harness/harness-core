package io.harness.perpetualtask.internal;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
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

public class RecentlyDisconnectedDelegateHandlerTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String delegateId = "DELEGATE_ID";
  private Delegate delegate;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock PerpetualTaskRecordDao perpetualTaskRecordDao;
  @InjectMocks RecentlyDisconnectedDelegateHandler handler;

  @Before
  public void setUp() {
    delegate = Delegate.builder().accountId(accountId).uuid(delegateId).build();
    when(perpetualTaskRecordDao.resetDelegateId(eq(accountId), eq(delegateId))).thenReturn(true);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testHandle() {
    handler.handle(delegate);
    verify(perpetualTaskRecordDao).resetDelegateId(eq(accountId), eq(delegateId));
  }
}
